package album.com

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity: AppCompatActivity() {

    var IMAGE_FROM_ALBUM = 0
    var photoUri: Uri? = null
    var storage: FirebaseStorage? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        var addPhoto = Intent(Intent.ACTION_PICK)
        addPhoto.type = "image/*"
        startActivityForResult(addPhoto, IMAGE_FROM_ALBUM)

        addphoto_button_upload.setOnClickListener {
            uploadPhotoInFirestorage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            addphoto_image.setImageURI(photoUri)
        }
        else {
            finish()
        }
    }

    fun uploadPhotoInFirestorage() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Загружаем Ваше фото в галерею!")
        progressDialog.setCancelable(false)
        progressDialog.show()

        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()
            contentDTO.imageUrl = uri.toString()

            contentDTO.uid = auth?.currentUser?.uid

            contentDTO.userId = auth?.currentUser?.email

            contentDTO.explain = addphoto_edit_explain.text.toString()

            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("gallery")?.document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}