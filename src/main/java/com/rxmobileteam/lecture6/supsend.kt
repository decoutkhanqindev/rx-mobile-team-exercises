package com.rxmobileteam.lecture6

import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@JvmInline
value class UserId(val id: Int)

data class User(
  val id: UserId,
  val name: String,
)

data class UserDetails(
  val id: UserId,
  val email: String,
  val phone: String,
)

data class Post(
  val id: String,
  val title: String,
  val body: String,
  val userId: UserId,
)

data class UserAndPosts(
  val user: User,
  val posts: List<Post>,
)

data class UserAndDetails(
  val user: User,
  val details: UserDetails,
)

interface UserRepository {
  /**
   * Find a user by id.
   * @return the user if found, null otherwise.
   */
  suspend fun findUserById(id: UserId): User?

  /**
   * Find all posts by user id.
   * @return the list of posts if found, empty list otherwise.
   */
  suspend fun getPostsByUserId(id: UserId): List<Post>

  /**
   * Find a user by id and all posts by that user.
   * @return the user and all posts if found, null otherwise.
   */
  suspend fun findUserAndPostsById(id: UserId): UserAndPosts?

  /**
   * Find a user by id and all details by that user.
   * @return the user and all details if found, null otherwise.
   */
  suspend fun findUserAndUserDetailsById(id: UserId): UserAndDetails?
}

interface UserApi {
  suspend fun findUserById(id: UserId): User?
  suspend fun findDetailsByUser(id: UserId): UserDetails?
  suspend fun getPostsByUser(user: User): List<Post>
}

// ------------------------------------------------------------------------------------------

internal class RealUserRepository(
  private val userApi: UserApi,
  private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {

  // TODO: Call userApi's methods on ioDispatcher
  override suspend fun findUserById(id: UserId): User? {
    return withContext(ioDispatcher) {
      userApi.findUserById(id)
    }
  }

  // TODO: Call userApi's methods on ioDispatcher
  override suspend fun getPostsByUserId(id: UserId): List<Post> {
    val user: User? = this.findUserById(id)
    return withContext(ioDispatcher) {
      user?.let { userApi.getPostsByUser(user) } ?: emptyList()
    }
  }

  // TODO: Call userApi's methods on ioDispatcher
  override suspend fun findUserAndPostsById(id: UserId): UserAndPosts? {
    val user: User? = this.findUserById(id)
    val posts: List<Post> = this.getPostsByUserId(id)
    return withContext(ioDispatcher) {
      user?.let { UserAndPosts(user, posts) }
    }
  }

  // TODO: Call userApi's methods on ioDispatcher
  override suspend fun findUserAndUserDetailsById(id: UserId): UserAndDetails? {
    val user: User? = this.findUserById(id)
    val userDetails: UserDetails? = userApi.findDetailsByUser(id)
    return withContext(ioDispatcher) {
      user?.let { userDetails?.let { UserAndDetails(user, userDetails) } }
    }
  }
}

// ------------------------------------------------------------------------------------------

fun provideUserRepository(): UserRepository = RealUserRepository(
  userApi = object : UserApi {
    val users = listOf(
      User(UserId(1), "Leanne Graham"),
      User(UserId(2), "Ervin Howell"),
      User(UserId(3), "Clementine Bauch"),
      User(UserId(4), "Patricia Lebsack"),
      User(UserId(5), "Chelsey Dietrich"),
    )
    val userDetails = users.map {
      UserDetails(
        id = it.id,
        email = "${it.name.replace(" ", ".").lowercase()}@gmail.com",
        phone = "+1-770-736-8031",
      )
    }
    val posts = users.map { user ->
      List(10) {
        Post(
          id = UUID.randomUUID().toString(),
          title = "Title #${it} of ${user.name}",
          body = "Body #${it} of ${user.name}",
          userId = user.id,
        )
      }
    }

    override suspend fun findUserById(id: UserId): User? = users.find { it.id == id }.also { delay(500) }

    override suspend fun findDetailsByUser(id: UserId): UserDetails? =
      userDetails.find { it.id == id }.also { delay(500) }

    override suspend fun getPostsByUser(user: User): List<Post> =
      posts.find { it.firstOrNull()?.userId == user.id }.orEmpty().also { delay(500) }
  },
  ioDispatcher = Dispatchers.IO,
)


fun main() = runBlocking {
  val userRepository = provideUserRepository()

  println("findUserById")
  println(userRepository.findUserById(UserId(1)))
  println(userRepository.findUserById(UserId(100)))
  println("-".repeat(80))

  println("getPostsByUserId")
  println(userRepository.getPostsByUserId(UserId(1)))
  println(userRepository.getPostsByUserId(UserId(100)))
  println("-".repeat(80))

  println("findUserAndPostsById")
  println(userRepository.findUserAndPostsById(UserId(1)))
  println(userRepository.findUserAndPostsById(UserId(100)))
  println("-".repeat(80))

  println("findUserAndUserDetailsById")
  println(userRepository.findUserAndUserDetailsById(UserId(1)))
  println(userRepository.findUserAndUserDetailsById(UserId(100)))
  println("-".repeat(80))
}
