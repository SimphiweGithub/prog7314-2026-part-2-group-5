package com.divitiae.pulsesync.data.repository

import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.ArticleDao
import com.divitiae.pulsesync.data.local.seed.SeedData
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toEntity
import com.divitiae.pulsesync.data.mapper.toLinkEntities
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first article access. The UI observes Room; [refresh] pulls from the
 * API and overwrites the cache. [ensureSeeded] loads bundled sample content on
 * first launch so the feed is never empty before the API is live.
 */
class ArticleRepository(
    private val api: PulseSyncApi,
    private val articleDao: ArticleDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeFeed(categorySlug: String?): Flow<List<Article>> {
        val source =
            if (categorySlug.isNullOrBlank()) articleDao.observeAll()
            else articleDao.observeByCategory(categorySlug)
        return source.map { rows -> rows.map { it.toDomain() } }
    }

    fun observeArticle(id: String): Flow<Article?> =
        articleDao.observeById(id).map { it?.toDomain() }

    suspend fun getArticle(id: String): Article? = withContext(io) {
        articleDao.findById(id)?.toDomain()
    }

    suspend fun ensureSeeded() = withContext(io) {
        if (articleDao.count() == 0) writeArticles(SeedData.articles, System.currentTimeMillis())
    }

    suspend fun refresh(categorySlug: String? = null): Result<Unit> = withContext(io) {
        when (val result = safeApiCall { api.getFeed(category = categorySlug) }) {
            is Result.Success -> {
                writeArticles(result.data.articles.map { it.toDomain() }, System.currentTimeMillis())
                Result.Success(Unit)
            }

            is Result.Failure -> result
        }
    }

    suspend fun search(query: String): List<Article> = withContext(io) {
        articleDao.search(query).map { it.toDomain() }
    }

    suspend fun setBookmarked(id: String, saved: Boolean) = withContext(io) {
        articleDao.setBookmarked(id, saved)
    }

    private suspend fun writeArticles(articles: List<Article>, now: Long) {
        articleDao.upsertArticles(articles.map { it.toEntity(now) })
        articles.forEach { article ->
            articleDao.deleteLinksFor(article.id)
            articleDao.upsertLinks(article.toLinkEntities())
        }
    }
}
