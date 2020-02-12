package album.com

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.activity_comment.view.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity: AppCompatActivity() {

    var contentUid: String? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        contentUid = intent.getStringExtra("contentUid")
        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        comment_button_send.setOnClickListener {
            sendComments()
        }

    }

    inner class CommentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var comments: ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            FirebaseFirestore.getInstance()
                .collection("gallery")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }


        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_comment, p0, false)
            return CustomViewHolder(view)
        }
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = p0.itemView
            view.commentviewitem_textview_comment.text = comments[p1].comment
            view.commentviewitem_username_profile.text = comments[p1].userId

            firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                if (documentSnapshot.data != null) {
                    var url = documentSnapshot.data!!["images"]
                    Glide.with(this@CommentActivity).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                }
            }
        }
    }

    fun sendComments() {
        var comment = ContentDTO.Comment()
        comment.userId = FirebaseAuth.getInstance().currentUser?.email
        comment.uid = FirebaseAuth.getInstance().currentUser?.uid
        comment.comment = comment_edit_message.text.toString()
        comment.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("gallery").document(contentUid!!).collection("comments").document().set(comment)
        comment_edit_message.setText("")
    }
}