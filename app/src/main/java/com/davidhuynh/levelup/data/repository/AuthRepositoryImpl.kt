package com.davidhuynh.levelup.data.repository

import com.davidhuynh.levelup.data.local.dao.UserDao
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.mapper.toDomain
import com.davidhuynh.levelup.data.prefs.SessionStore
import com.davidhuynh.levelup.domain.logic.EmailValidator
import com.davidhuynh.levelup.domain.logic.PasswordPolicy
import com.davidhuynh.levelup.domain.model.Session
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.security.PasswordHash
import com.davidhuynh.levelup.domain.security.PasswordHasher
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val hasher: PasswordHasher,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
    private val statsRecomputer: DerivedDataRecomputer,

    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,

    private val onUserCreated: suspend (String) -> Unit = {},
) : AuthRepository {

    override val session: Flow<Session?> = sessionStore.session.map { stored ->
        if (stored == null) return@map null
        if (userDao.findById(stored.userId) == null) {
            sessionStore.clear()
            null
        } else {
            stored
        }
    }

    override suspend fun signUp(
        email: String,
        displayName: String,
        password: String,
        confirmPassword: String,
    ): DataResult<User> {
        val normalizedEmail = EmailValidator.normalize(email)
        val name = displayName.trim()

        if (name.isEmpty()) return DataResult.Failure("Enter a display name", FIELD_NAME)
        if (name.length > 40) return DataResult.Failure("Keep the name under 40 characters", FIELD_NAME)
        if (!EmailValidator.isValid(normalizedEmail)) {
            return DataResult.Failure("Enter a valid email address", FIELD_EMAIL)
        }

        val violations = PasswordPolicy.validate(password)
        if (violations.isNotEmpty()) {
            return DataResult.Failure(violations.joinToString("\n") { it.message }, FIELD_PASSWORD)
        }
        if (password != confirmPassword) {
            return DataResult.Failure("Passwords do not match", FIELD_CONFIRM)
        }
        if (userDao.emailExists(normalizedEmail)) {
            return DataResult.Failure("An account with that email already exists", FIELD_EMAIL)
        }

        val hashed = withContext(cryptoDispatcher) { hasher.hash(password) }
        val now = clock.nowMillis()
        val entity = UserEntity(
            id = tokens.newId(),
            email = normalizedEmail,
            displayName = name,
            passwordHash = hashed.hash,
            passwordSalt = hashed.salt,
            passwordIterations = hashed.iterations,
            avatarEmoji = null,
            weightUnit = WeightUnit.LB.name,
            createdAt = now,
            isDemo = false,
        )

        return try {
            userDao.insert(entity)
            statsRecomputer.recomputeUserStats(entity.id)
            onUserCreated(entity.id)
            startSession(entity.id)
            DataResult.Success(entity.toDomain())
        } catch (e: android.database.sqlite.SQLiteConstraintException) {

            DataResult.Failure("An account with that email already exists", FIELD_EMAIL)
        }
    }

    override suspend fun signIn(email: String, password: String): DataResult<User> {
        val normalizedEmail = EmailValidator.normalize(email)
        if (normalizedEmail.isEmpty() || password.isEmpty()) {
            return DataResult.Failure("Enter your email and password")
        }

        val user = userDao.findByEmail(normalizedEmail)
            ?: return DataResult.Failure(INVALID_CREDENTIALS)

        val stored = PasswordHash(user.passwordHash, user.passwordSalt, user.passwordIterations)
        val matches = withContext(cryptoDispatcher) { hasher.verify(password, stored) }
        if (!matches) return DataResult.Failure(INVALID_CREDENTIALS)

        if (hasher.needsRehash(stored)) {
            val upgraded = withContext(cryptoDispatcher) { hasher.hash(password) }
            userDao.update(
                user.copy(
                    passwordHash = upgraded.hash,
                    passwordSalt = upgraded.salt,
                    passwordIterations = upgraded.iterations,
                )
            )
        }

        startSession(user.id)
        return DataResult.Success(user.toDomain())
    }

    override suspend fun signOut() {

        sessionStore.clear()
    }

    override suspend fun refreshSession() {
        sessionStore.touch()
    }

    override fun observeUser(userId: String): Flow<User?> =
        userDao.observeById(userId).map { it?.toDomain() }

    override suspend fun getUser(userId: String): User? = userDao.findById(userId)?.toDomain()

    override suspend fun updateWeightUnit(userId: String, unit: WeightUnit) {
        val user = userDao.findById(userId) ?: return
        userDao.update(user.copy(weightUnit = unit.name))
    }

    override suspend fun updateProfile(
        userId: String,
        displayName: String,
        avatarEmoji: String?,
    ): DataResult<User> {
        val name = displayName.trim()
        if (name.isEmpty()) return DataResult.Failure("Enter a display name", FIELD_NAME)
        val user = userDao.findById(userId) ?: return DataResult.Failure("Account not found")
        val updated = user.copy(displayName = name, avatarEmoji = avatarEmoji)
        userDao.update(updated)
        return DataResult.Success(updated.toDomain())
    }

    private suspend fun startSession(userId: String) {
        sessionStore.save(userId, tokens.newSessionToken())
    }

    private companion object {
        const val INVALID_CREDENTIALS = "Incorrect email or password"
        const val FIELD_NAME = "displayName"
        const val FIELD_EMAIL = "email"
        const val FIELD_PASSWORD = "password"
        const val FIELD_CONFIRM = "confirmPassword"
    }
}
