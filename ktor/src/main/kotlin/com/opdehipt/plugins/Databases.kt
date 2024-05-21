package com.opdehipt.plugins

import com.opdehipt.IdType
import com.opdehipt.native_push.PushSystem
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

private lateinit var database: Database

/**
 * Configures database connections based on application environment properties.
 *
 * @param idType the type of ID used in the database (Long, UUID, String).
 */
internal fun <T> Application.configureDatabases(idType: IdType<T>) {
    // Retrieve database connection properties from environment configuration
    val postgresHost = environment.config.propertyOrNull("postgresHost")?.getString()
    val postgresDb = environment.config.propertyOrNull("postgresDb")?.getString()
    val postgresUser = environment.config.propertyOrNull("postgresUser")?.getString()
    val postgresPassword = environment.config.propertyOrNull("postgresPassword")?.getString()
    val mysqlHost = environment.config.propertyOrNull("mysqlHost")?.getString()
    val mysqlDb = environment.config.propertyOrNull("mysqlDb")?.getString()
    val mysqlUser = environment.config.propertyOrNull("mysqlUser")?.getString()
    val mysqlPassword = environment.config.propertyOrNull("mysqlPassword")?.getString()
    val mariaHost = environment.config.propertyOrNull("mariaHost")?.getString()
    val mariaDb = environment.config.propertyOrNull("mariaDb")?.getString()
    val mariaUser = environment.config.propertyOrNull("mariaUser")?.getString()
    val mariaPassword = environment.config.propertyOrNull("mariaPassword")?.getString()

    // Connect to the appropriate database based on provided properties
    if (postgresHost != null && postgresDb != null && postgresUser != null && postgresPassword != null) {
        database = Database.connect(
            url = "jdbc:postgresql://$postgresHost/$postgresDb",
            user = postgresUser,
            driver = "org.postgresql.Driver",
            password = postgresPassword
        )
    }
    else if (mysqlHost != null && mysqlDb != null && mysqlUser != null && mysqlPassword != null) {
        database = Database.connect(
            url = "jdbc:mysql://$mysqlHost/$mysqlDb",
            user = mysqlUser,
            driver = "com.mysql.cj.jdbc.Driver",
            password = mysqlPassword
        )
    }
    else if (mariaHost != null && mariaDb != null && mariaUser != null && mariaPassword != null) {
        database = Database.connect(
            url = "jdbc:mariadb://$mariaHost/$mariaDb",
            user = mariaUser,
            driver = "org.mariadb.jdbc.Driver",
            password = mariaPassword
        )
    }

    // Determine the table based on the ID type
    val table = when (idType) {
        IdType.Long -> NotificationTokenLong
        IdType.UUID -> NotificationTokenUUID
        IdType.String -> NotificationTokenString
    }

    // Create the table schema if it doesn't already exist
    transaction(database) {
        SchemaUtils.create(table)
    }
}

/**
 * Retrieves notification tokens for a user with a UUID ID.
 *
 * @param userId the UUID of the user.
 * @return a list of notification tokens and their associated push systems.
 */
internal suspend fun getNotificationTokens(userId: UUID) = getNotificationTokens(userId, NotificationTokenUUID)

/**
 * Retrieves notification tokens for a user with a Long ID.
 *
 * @param userId the Long ID of the user.
 * @return a list of notification tokens and their associated push systems.
 */
internal suspend fun getNotificationTokens(userId: Long) = getNotificationTokens(userId, NotificationTokenLong)

/**
 * Retrieves notification tokens for a user with a String ID.
 *
 * @param userId the String ID of the user.
 * @return a list of notification tokens and their associated push systems.
 */
internal suspend fun getNotificationTokens(userId: String) = getNotificationTokens(userId, NotificationTokenString)

/**
 * Generic method to retrieve notification tokens for a user.
 *
 * @param userId the ID of the user.
 * @param table the table representing the notification tokens.
 * @return a list of notification tokens and their associated push systems.
 */
private suspend fun <T> getNotificationTokens(userId: T, table: NotificationToken<T>): List<Pair<String, PushSystem>> =
    suspendedTransactionAsync {
        table.selectAll().where { table.userId eq userId }.map { it[table.token] to it[table.system] }
    }.await()

/**
 * Inserts a notification token for a user with a UUID ID.
 *
 * @param system the push system.
 * @param token the notification token.
 * @param userId the UUID of the user.
 * @return the inserted token ID.
 */
internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: UUID) =
    insertNotificationToken(system, token, userId, NotificationTokenUUID)

/**
 * Inserts a notification token for a user with a Long ID.
 *
 * @param system the push system.
 * @param token the notification token.
 * @param userId the Long ID of the user.
 * @return the inserted token ID.
 */
internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: Long) =
    insertNotificationToken(system, token, userId, NotificationTokenLong)

/**
 * Inserts a notification token for a user with a String ID.
 *
 * @param system the push system.
 * @param token the notification token.
 * @param userId the String ID of the user.
 * @return the inserted token ID.
 */
internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: String) =
    insertNotificationToken(system, token, userId, NotificationTokenString)

/**
 * Generic method to insert a notification token for a user.
 *
 * @param system the push system.
 * @param token the notification token.
 * @param userId the ID of the user.
 * @param table the table representing the notification tokens.
 * @return the inserted token ID.
 */
private suspend fun <T> insertNotificationToken(system: PushSystem, token: String, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.insert {
            it[table.system] = system
            it[table.token] = token
            it[table.userId] = userId
        }
    }.await()[table.tokenId]

/**
 * Updates a notification token for a user with a UUID ID.
 *
 * @param tokenId the ID of the token.
 * @param system the push system.
 * @param token the notification token.
 * @param userId the UUID of the user.
 * @return true if the update was successful, false otherwise.
 */
internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: UUID) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenUUID)

/**
 * Updates a notification token for a user with a Long ID.
 *
 * @param tokenId the ID of the token.
 * @param system the push system.
 * @param token the notification token.
 * @param userId the Long ID of the user.
 * @return true if the update was successful, false otherwise.
 */
internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: Long) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenLong)

/**
 * Updates a notification token for a user with a String ID.
 *
 * @param tokenId the ID of the token.
 * @param system the push system.
 * @param token the notification token.
 * @param userId the String ID of the user.
 * @return true if the update was successful, false otherwise.
 */
internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: String) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenString)

/**
 * Generic method to update a notification token for a user.
 *
 * @param tokenId the ID of the token.
 * @param system the push system.
 * @param token the notification token.
 * @param userId the ID of the user.
 * @param table the table representing the notification tokens.
 * @return true if the update was successful, false otherwise.
 */
private suspend fun <T> updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.update({
            table.userId eq userId and (table.tokenId eq tokenId)
        }) {
            it[table.system] = system
            it[table.token] = token
        } != 0
    }.await()

/**
 * Deletes a notification token for a user with a UUID ID.
 *
 * @param tokenId the ID of the token.
 * @param userId the UUID of the user.
 * @return true if the deletion was successful, false otherwise.
 */
internal suspend fun deleteNotificationToken(tokenId: UUID, userId: UUID) =
    deleteNotificationToken(tokenId, userId, NotificationTokenUUID)

/**
 * Deletes a notification token for a user with a Long ID.
 *
 * @param tokenId the ID of the token.
 * @param userId the Long ID of the user.
 * @return true if the deletion was successful, false otherwise.
 */
internal suspend fun deleteNotificationToken(tokenId: UUID, userId: Long) =
    deleteNotificationToken(tokenId, userId, NotificationTokenLong)

/**
 * Deletes a notification token for a user with a String ID.
 *
 * @param tokenId the ID of the token.
 * @param userId the String ID of the user.
 * @return true if the deletion was successful, false otherwise.
 */
internal suspend fun deleteNotificationToken(tokenId: UUID, userId: String) =
    deleteNotificationToken(tokenId, userId, NotificationTokenString)

/**
 * Generic method to delete a notification token for a user.
 *
 * @param tokenId the ID of the token.
 * @param userId the ID of the user.
 * @param table the table representing the notification tokens.
 * @return true if the deletion was successful, false otherwise.
 */
private suspend fun <T> deleteNotificationToken(tokenId: UUID, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.deleteWhere {
            table.userId eq userId and (table.tokenId eq tokenId)
        } != 0
    }.await()

/**
 * Abstract class representing the notification token table.
 *
 * @param T the type of the user ID.
 */
private abstract class NotificationToken<T>: Table("notification_token") {
    val token = text("token")
    val system = enumeration<PushSystem>("system")
    val tokenId = uuid("token_id").autoGenerate()
    init {
        uniqueIndex(token, system)
    }
    abstract val userId: Column<T>
}

/**
 * Object representing the notification token table with Long user ID.
 */
private object NotificationTokenLong: NotificationToken<Long>() {
    override val userId = long("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}

/**
 * Object representing the notification token table with UUID user ID.
 */
private object NotificationTokenUUID: NotificationToken<UUID>() {
    override val userId = uuid("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}

/**
 * Object representing the notification token table with String user ID.
 */
private object NotificationTokenString: NotificationToken<String>() {
    override val userId = text("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}
