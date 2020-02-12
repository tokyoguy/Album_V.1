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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_person.view.*

class UserFragment: Fragment() {
    var fragmentView: View? = null
    var uid: String? = null
    var firestore: FirebaseFirestore? = null
    var auth: FirebaseAuth? = null
    var currentUserUid: String? = null
    companion object { var PROFILE_IMAGE_CODE = 228 }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)

        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if (uid == currentUserUid) {
            fragmentView?.person_profile_exit?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }
        else {
            var mainactivity = (activity as MainActivity)
            mainactivity.person_profile_exit?.visibility = View.GONE
        }

        fragmentView?.person_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.person_recyclerview?.layoutManager = LinearLayoutManager(activity)

        fragmentView?.person_profile_image?.setOnClickListener {
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            activity?.startActivityForResult(photoIntent, PROFILE_IMAGE_CODE)
        }
        getProfileImages()
        return fragmentView
    }
    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("gallery")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener
                for(snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    contentUidList.add(snapshot.id)
                }
                fragmentView?.person_post_counter?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_person, p0, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholder = (p0 as CustomViewHolder).itemView

            viewholder.personitem_profile_textview.text = contentDTOs[p1].userId
            viewholder.personitem_explain_textview.text = contentDTOs[p1].explain
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).into(viewholder.personitem_imageview_content)
            viewholder.personitem_favoritecounter_textview.text = "" + contentDTOs[p1].favoriteCount

            viewholder.personitem_comment_imageview.setOnClickListener {v ->
                var intent = Intent(Intent(v.context, CommentActivity::class.java))
                intent.putExtra("contentUid", contentUidList[p1])
                startActivity(intent)
            }

            firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                if (documentSnapshot.data != null) {
                    var url = documentSnapshot.data!!["images"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(viewholder.personitem_profile_image!!)
                }
            }
        }
    }

    fun getProfileImages(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null) {
                var url = documentSnapshot.data!!["images"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.person_profile_image!!)
            }
        }
    }
}