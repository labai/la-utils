import com.google.gson.GsonBuilder

/*
 * @author Augustus
 * created on 2023-02-18
*/

private val gson = GsonBuilder().setPrettyPrinting().create()

internal fun printJson(obj: Any) {
    println(gson.toJson(obj))
}
