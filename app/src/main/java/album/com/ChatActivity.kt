package album.com

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_chat.*
import kotlinx.android.synthetic.main.item_chat.view.*

class ChatActivity: AppCompatActivity() {

    var firestore: FirebaseFirestore? = null
    var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        chat_recyclerview.adapter = ChatRecyclerViewAdapter()
        chat_recyclerview.layoutManager = LinearLayoutManager(this)

        chat_button_send.setOnClickListener {
            sendMessages()
        }
    }

    inner class ChatRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var chats: ArrayList<ContentDTO.Chat> = arrayListOf()
        init {
            FirebaseFirestore.getInstance().collection("messages").orderBy("timestamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    chats.clear()
                    if (querySnapshot == null) return@addSnapshotListener
                for (snapshot in querySnapshot.documents) {
                    chats.add(snapshot.toObject(ContentDTO.Chat::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_chat, p0, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return chats.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = p0.itemView
            view.chatviewitem_username_profile.text = chats[p1].userId
            view.chatviewitem_textview_comment.text = chats[p1].message

            firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                if (documentSnapshot.data != null) {
                    var url = documentSnapshot.data!!["images"]
                    Glide.with(this@ChatActivity).load(url).apply(RequestOptions().circleCrop()).into(view.chatviewitem_imageview_profile)
                }
            }
        }
    }

    fun sendMessages() {
        var messages = ContentDTO.Chat()

        messages.uid = FirebaseAuth.getInstance().currentUser?.uid
        messages.userId = FirebaseAuth.getInstance().currentUser?.email
        messages.message = chat_edit_message.text.toString()
        messages.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("messages").document().set(messages)
        chat_edit_message.setText("")
    }
}
