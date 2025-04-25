import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class DataWrapper(val data: Map<String, Any>) : Parcelable