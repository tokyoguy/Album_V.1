package album.com

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import kotlinx.android.synthetic.main.item_gallery.view.*

class GalleryFragment: Fragment() {

    var firestore: FirebaseFirestore? = null
    var uid: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_gallery, container, false)

        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.galleryviewfragment_recyclerview.adapter = GalleryViewRecyclerViewAdapter()
        view.galleryviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

inner class GalleryViewRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
    var contentUidList: ArrayList<String> = arrayListOf()

    init {
        firestore?.collection("gallery")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            contentDTOs.clear()
            contentUidList.clear()
            if (querySnapshot == null) return@addSnapshotListener
            for(snapshot in querySnapshot!!.documents) {
                var item = snapshot.toObject(ContentDTO::class.java)
                contentDTOs.add(item!!)
                contentUidList.add(snapshot.id)
            }
            notifyDataSetChanged()
            }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        var view = LayoutInflater.from(p0.context).inflate(R.layout.item_gallery, p0, false)
        return CustomViewHolder(view)
    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int {
        return contentDTOs.size
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        var viewholder = (p0 as CustomViewHolder).itemView

        viewholder.galleryitem_profile_textview.text = contentDTOs[p1].userId
        viewholder.galleryitem_explain_textview.text = contentDTOs[p1].explain
        Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).into(viewholder.galleryitem_imageview_content)
        viewholder.galleryitem_favoritecounter_textview.text = "" + contentDTOs[p1].favoriteCount
        viewholder.galleryitem_favorite_imageview.setOnClickListener {
            likeEvent(p1)
        }
        if (contentDTOs[p1].favorites.containsKey(uid)) {
            viewholder.galleryitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_active)
        }
        else {
            viewholder.galleryitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_neactive)
        }

        viewholder.galleryitem_comment_imageview.setOnClickListener {v ->
            var intent = Intent(Intent(v.context, CommentActivity::class.java))
            intent.putExtra("contentUid", contentUidList[p1])
            startActivity(intent)
        }
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null) {
                var url = documentSnapshot.data!!["images"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(viewholder.galleryitem_profile_image!!)
            }
        }

        viewholder.galleryitem_profile_image.setOnClickListener {
            var userFragment = UserFragment()
            var bundle = Bundle()
            bundle.putString("destinationUid", contentDTOs[p1].uid)
            userFragment.arguments = bundle
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, userFragment)?.commit()
        }

    }
    fun likeEvent(p1: Int) {
        var docRef = firestore?.collection("gallery")?.document(contentUidList[p1])
        firestore?.runTransaction { transaction ->
            var contentDTO = transaction.get(docRef!!).toObject(ContentDTO::class.java)
            if (contentDTO!!.favorites.containsKey(uid)) {
                contentDTO.favoriteCount = contentDTO.favoriteCount -1
                contentDTO.favorites.remove(uid)
            }
            else {
                contentDTO.favoriteCount = contentDTO.favoriteCount +1
                contentDTO.favorites[uid!!] = true
                likeNotification(contentDTOs[p1].uid!!)
            }
            transaction.set(docRef, contentDTO)
        }
    }
    fun likeNotification(destinationUid: String) {
        var message = FirebaseAuth.getInstance().currentUser?.email + " " +getString(R.string.likes_notifications)
        FcmPush.instance.sendMessage(destinationUid, "Ваше фото оценили!", message)
    }
}
}