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

internal fun <T> Application.configureDatabases(idType: IdType<T>) {
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
    val table = when (idType) {
        IdType.Long -> NotificationTokenLong
        IdType.UUID -> NotificationTokenUUID
        IdType.String -> NotificationTokenString
    }

    transaction(database) {
        SchemaUtils.create(table)
    }
}

internal suspend fun getNotificationTokens(userId: UUID) = getNotificationTokens(userId, NotificationTokenUUID)
internal suspend fun getNotificationTokens(userId: Long) = getNotificationTokens(userId, NotificationTokenLong)
internal suspend fun getNotificationTokens(userId: String) = getNotificationTokens(userId, NotificationTokenString)

private suspend fun <T> getNotificationTokens(userId: T, table: NotificationToken<T>): List<Pair<String, PushSystem>> =
    suspendedTransactionAsync {
        table.select { table.userId eq userId }.map { it[table.token] to it[table.system] }
    }.await()

internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: UUID) =
    insertNotificationToken(system, token, userId, NotificationTokenUUID)
internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: Long) =
    insertNotificationToken(system, token, userId, NotificationTokenLong)
internal suspend fun insertNotificationToken(system: PushSystem, token: String, userId: String) =
    insertNotificationToken(system, token, userId, NotificationTokenString)

private suspend fun <T> insertNotificationToken(system: PushSystem, token: String, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.insert {
            it[table.system] = system
            it[table.token] = token
            it[table.userId] = userId
        }
    }.await()[table.tokenId]

internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: UUID) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenUUID)
internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: Long) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenLong)
internal suspend fun updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: String) =
    updateNotificationToken(tokenId, system, token, userId, NotificationTokenString)

private suspend fun <T> updateNotificationToken(tokenId: UUID, system: PushSystem, token: String, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.update({
            table.userId eq userId and (table.tokenId eq tokenId)
        }) {
            it[table.system] = system
            it[table.token] = token
        } != 0
    }.await()

internal suspend fun deleteNotificationToken(tokenId: UUID, userId: UUID) =
    deleteNotificationToken(tokenId, userId, NotificationTokenUUID)
internal suspend fun deleteNotificationToken(tokenId: UUID, userId: Long) =
    deleteNotificationToken(tokenId, userId, NotificationTokenLong)
internal suspend fun deleteNotificationToken(tokenId: UUID, userId: String) =
    deleteNotificationToken(tokenId, userId, NotificationTokenString)

private suspend fun <T> deleteNotificationToken(tokenId: UUID, userId: T, table: NotificationToken<T>) =
    suspendedTransactionAsync {
        table.deleteWhere {
            table.userId eq userId and (table.tokenId eq tokenId)
        } != 0
    }.await()

private abstract class NotificationToken<T>: Table("notification_token") {
    val token = text("token")
    val system = enumeration<PushSystem>("system")
    val tokenId = uuid("token_id").autoGenerate()
    init {
        uniqueIndex(token, system)
    }
    abstract val userId: Column<T>
}

private object NotificationTokenLong: NotificationToken<Long>() {
    override val userId = long("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}

private object NotificationTokenUUID: NotificationToken<UUID>() {
    override val userId = uuid("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}

private object NotificationTokenString: NotificationToken<String>() {
    override val userId = text("user_id")
    override val primaryKey = PrimaryKey(userId, tokenId)
}
