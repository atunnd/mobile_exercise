package com.example.gallery_detection

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.example.gallery_detection.databinding.ActivityMainBinding
import com.example.gallery_detection.databinding.DisplayedImageBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ImageSearchViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        observeViewModel()
        handleLoadingStates()
    }

    private fun setupRecyclerView() {
        imageAdapter = ImageAdapter()
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = imageAdapter
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
        binding.btnSearch.setOnClickListener { performSearch() }
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.searchImages(query)
            binding.recyclerView.scrollToPosition(0)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.imageFlow.collectLatest { imageAdapter.submitData(it) }
        }
    }

    private fun handleLoadingStates() {
        lifecycleScope.launch {
            imageAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.progressBar.isVisible = loadStates.refresh is LoadState.Loading || loadStates.append is LoadState.Loading
                (loadStates.refresh as? LoadState.Error)?.let {
                    Toast.makeText(this@MainActivity, "Lỗi tải ảnh: ${it.error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                (loadStates.append as? LoadState.Error)?.let {
                    Toast.makeText(this@MainActivity, "Lỗi tải thêm ảnh: ${it.error.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class ImageSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val currentQuery = MutableStateFlow<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pixabay.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(PixabayApiService::class.java)

    interface PixabayApiService {
        @GET(".")
        suspend fun searchImages(
            @Query("key") apiKey: String = BuildConfig.API_KEY,
            @Query("q") query: String,
            @Query("image_type") imageType: String = "photo",
            @Query("page") page: Int,
            @Query("per_page") perPage: Int
        ): PixabayResponse
    }

    fun searchImages(query: String) {
        currentQuery.value = query.trim().takeIf { it.isNotEmpty() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val imageFlow: Flow<PagingData<ProcessedImage>> = currentQuery
        .filterNotNull()
        .flatMapLatest { query ->
            Pager(PagingConfig(pageSize = 10)) {
                object : PagingSource<Int, ImageItem>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ImageItem> {
                        val page = params.key ?: 1
                        return try {
                            val response = api.searchImages(query = query, page = page, perPage = 10)
                            LoadResult.Page(response.hits ?: emptyList(), prevKey = null, nextKey = null)
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }

                    override fun getRefreshKey(state: PagingState<Int, ImageItem>): Int? = null
                }
            }.flow.map { pagingData ->
            pagingData.mapAsync(Dispatchers.IO) { item ->
                val labels = analyzeImage(item.webformatURL)
                ProcessedImage(item.id, item.webformatURL, item.tags, item.user, labels)
               }
            }
        }.cachedIn(viewModelScope)

    private suspend fun analyzeImage(url: String): List<String> {
        return try {
            val context = getApplication<Application>()
            val request = ImageRequest.Builder(context = context)
                .data(url)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return emptyList()
            val image = InputImage.fromBitmap(bitmap, 0)
            labeler.process(image).await().take(3).map { it.text }
        } catch (e: Exception) {
            println("Lỗi ảnh $url: ${e.message}")
            emptyList()
        }
    }
}

class ImageAdapter : PagingDataAdapter<ProcessedImage, ImageAdapter.ImageViewHolder>(IMAGE_COMPARATOR) {
    inner class ImageViewHolder(private val binding: DisplayedImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProcessedImage?) {
            if (item == null) {
                binding.imageView.setImageResource(R.drawable.ic_launcher_background)
            } else {
                binding.imageView.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_foreground)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ImageViewHolder(DisplayedImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val IMAGE_COMPARATOR = object : DiffUtil.ItemCallback<ProcessedImage>() {
            override fun areItemsTheSame(oldItem: ProcessedImage, newItem: ProcessedImage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ProcessedImage, newItem: ProcessedImage) = oldItem == newItem
        }
    }
}

private fun <T : Any, R : Any> PagingData<T>.mapAsync(
    context: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (T) -> R
): PagingData<R> {
    return this.map { item ->
        withContext(context) { transform(item) }
    }
}