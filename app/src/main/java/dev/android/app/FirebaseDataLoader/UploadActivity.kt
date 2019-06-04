package dev.android.app.FirebaseDataLoader

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import  dev.android.app.R
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.support.media.ExifInterface
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.android.gms.tasks.Task
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.widget.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.UploadTask
import java.io.*

import java.text.SimpleDateFormat
import java.util.*

private const val STORAGE_PERMISSION_REQUEST_ID = 10

class UploadActivity() : AppCompatActivity() {
    lateinit var context: Context

    companion object {
        var generateAlertDialogAgain: Boolean = false;
    }

    private var permissionsArray = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    var SELECT_IMAGE_ID: Int = 100
    val CAPTURE_IMAGE_ID: Int = 200
    var downloadUri: String = ""
    var filePath: String? = null
    lateinit var photoUri: Uri
    lateinit var dataForCapturedImage: ByteArray
    var flagToUploadCapturedImage: Boolean = false

    var fAuth = FirebaseAuth.getInstance()
    var user = fAuth.currentUser
    lateinit var uid: String
    lateinit var mStorage: StorageReference
    lateinit var fDatabase: DatabaseReference
    lateinit var fDatabaseStatistics: DatabaseReference
    lateinit var fDatabaseLinks: DatabaseReference
    var uploadSuccess: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        context = this
        uid = user!!.uid
        mStorage = FirebaseStorage.getInstance().getReference("present_menu_images")
        fDatabase = FirebaseDatabase.getInstance().getReference("owners")
        fDatabaseStatistics = FirebaseDatabase.getInstance().getReference("statistics")

        val uploadImageFromGallery = findViewById<Button>(R.id.selectPictureFromGallery)
        uploadImageFromGallery.setOnClickListener(
                { view: View? ->
                    val intent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    intent.type = "image/*"
                    intent.putExtra("return-data", true);
                    startActivityForResult(intent, SELECT_IMAGE_ID)
                })

        val captureImage = findViewById<Button>(R.id.takePictureButton)
        captureImage.setOnClickListener {
            takePicture()
        }

        val saveMenu = findViewById<Button>(R.id.uploadButton)
        saveMenu.setOnClickListener {
            val menuDescription = findViewById<TextView>(R.id.menuDescriptionTextView)
            val description = menuDescription.text.toString()
            if (uploadSuccess == 1) {
                if (description.length <= 50) {
                    uploadMenu(description, flagToUploadCapturedImage)
                } else {
                    Toast.makeText(this, "Maximum 50 characters allowed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Select image to upload", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun takePicture() {
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isAllPermissionsGiven(context, permissionsArray)) {
                        // Toast.makeText(context, "Permission are already provided", Toast.LENGTH_SHORT).show()
                        photoFile = createImageFile()
                    } else {
                        requestPermissions(permissionsArray, STORAGE_PERMISSION_REQUEST_ID)
                    }
                } else {
                    //Toast.makeText(context, "Permission are already provided", Toast.LENGTH_SHORT).show()
                    photoFile = createImageFile()
                }

            } catch (e: IOException) {
                Toast.makeText(this, "Error occured in taking photo", Toast.LENGTH_SHORT).show()
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "android.project.firebaseapplication.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, CAPTURE_IMAGE_ID)
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_Hmmss").format(Date())
        val fileName = getString(R.string.app_name) + " menu upload on - " + timeStamp
        //  val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val storageDirectory = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        var image = File.createTempFile(fileName, ".jpg", storageDirectory)
        filePath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var menuImage = findViewById<ImageView>(R.id.menuImageImageView)
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_IMAGE_ID) {
            if(data!=null) {
                var uri = data!!.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                menuImage.setImageBitmap(bitmap)
                filePath = uri.path
                //rotateImage(bitmap)
                var baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                dataForCapturedImage = baos.toByteArray();
                flagToUploadCapturedImage = true
                uploadSuccess = 1
            }
            else{
                Toast.makeText(this,"operation failed",Toast.LENGTH_SHORT).show()
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == CAPTURE_IMAGE_ID) {
            try {
                var bitmap = BitmapFactory.decodeFile(filePath)
                rotateImage(bitmap)
                flagToUploadCapturedImage = true
                uploadSuccess = 1
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setReducedImageSize(): Bitmap? {
        var menuImageView = findViewById<ImageView>(R.id.menuImageImageView)
        val targetImageViewWidth = menuImageView.width
        val targetIamgeViewHeight = menuImageView.height

        var bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true; //to just read dimentions not load whole image
        BitmapFactory.decodeFile(filePath, bmOptions)
        val cameraImageWidth = bmOptions.outWidth
        val cameraImageHeight = bmOptions.outHeight

        val scaleFactor = Math.min(cameraImageWidth / targetImageViewWidth, cameraImageHeight / targetIamgeViewHeight)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false //now load whole image

        val photoReducedSizeBitmap = BitmapFactory.decodeFile(filePath, bmOptions)
        return photoReducedSizeBitmap
    }

    private fun rotateImage(bitmap: Bitmap) {
        var menuImageView = findViewById<ImageView>(R.id.menuImageImageView)
        var exifInterface: ExifInterface? = null
        try {
            exifInterface = ExifInterface(filePath!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientation = exifInterface!!.getAttributeDouble(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED.toDouble())
        var matrix = Matrix()
        when (orientation) {
            (ExifInterface.ORIENTATION_ROTATE_90.toDouble()) -> matrix.setRotate(90F)
            (ExifInterface.ORIENTATION_ROTATE_180.toDouble()) -> matrix.setRotate(180F)
        }
        var rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        menuImageView.setImageBitmap(rotateBitmap)

        var baos = ByteArrayOutputStream()
        rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        dataForCapturedImage = baos.toByteArray();
    }

    private fun uploadMenu(menuDescription: String, flagToUploadCapturedImage: Boolean) {

        var progressDialog: ProgressDialog;
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("uploading menu...")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show();

        lateinit var uploadTask: UploadTask
        if (flagToUploadCapturedImage == false) {
            //val stream = FileInputStream(File(photoUri.toString().trim()))
            //uploadTask = mStorage.child(uid).putStream(stream)

        } else {
            uploadTask = mStorage.child(uid).putBytes(dataForCapturedImage);
        }
        //var uploadTask= mStorage.child(uid).putFile(photoUri)
        uploadTask.continueWithTask(object : Continuation<UploadTask.TaskSnapshot, Task<Uri>> {
            @Throws(Exception::class)
            override fun then(task: Task<UploadTask.TaskSnapshot>): Task<Uri> {
                if (!task.isSuccessful) {
                    throw task.getException()!!
                }
                return mStorage.child(uid).getDownloadUrl()
            }
        }).addOnCompleteListener(OnCompleteListener<Uri> { task ->
            if (task.isSuccessful) {
                downloadUri = task.result.toString()
                fDatabase.child(uid).child("latest_menu_url").setValue(downloadUri)
                fDatabase.child(uid).child("menu_description_text").setValue(menuDescription)
                Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show()

                fDatabase.child(uid).child("latest_menu_url").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.value.toString().isEmpty()) {
                            fDatabaseStatistics.child("present_menu_images_count").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onCancelled(p0: DatabaseError) {
                                    TODO("not implemented")
                                }

                                override fun onDataChange(p0: DataSnapshot) {

                                    var cnt = p0.value.toString().toInt()
                                    cnt++
                                    fDatabaseStatistics.child("present_menu_images_count").setValue(cnt)
                                    progressDialog.dismiss()
                                }
                            })
                        }
                    }
                })
                finish()
            } else {
                if (progressDialog.isShowing) {
                    progressDialog.dismiss();
                }
                Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show()
            }
        })
        uploadTask.addOnProgressListener(object : OnProgressListener<UploadTask.TaskSnapshot> {
            override fun onProgress(p0: UploadTask.TaskSnapshot?) {
//                var progress=(100.0*p0!!.bytesTransferred / p0!!.totalByteCount)
//                context.showToast("Uploading... $progress %")
//                progressBar.incrementProgressBy(progress.toInt())
                //   dialog.show()
            }
        })
    }

    fun isAllPermissionsGiven(context: Context, permissionArray: Array<String>): Boolean {
        var allPermissionsGiven = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allPermissionsGiven = false
        }
        return allPermissionsGiven
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_ID) {
            var allPermissionsGiven = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allPermissionsGiven = false
                }
            }
            if (!allPermissionsGiven) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    var shouldShowRequestPermissionRational = false
                    for (i in permissions.indices) {
                        shouldShowRequestPermissionRational = shouldShowRequestPermissionRational || shouldShowRequestPermissionRationale(permissions[i])
                    }
                    if (shouldShowRequestPermissionRational) {
                        showPermissionAlert()
                    } else {
                        Toast.makeText(this, "Go to apps settings and enable the storage permission to " + getString(R.string.app_name), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Go to apps settings and enable the storage permission to " + getString(R.string.app_name), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPermissionAlert() {
        val builder: AlertDialog.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = AlertDialog.Builder(this@UploadActivity, R.style.Theme_AppCompat_DayNight_Dialog)
        } else {
            this@UploadActivity.setTheme(R.style.Theme_AppCompat_DayNight_Dialog)
            builder = AlertDialog.Builder(this@UploadActivity)
        }
        builder.setTitle("Permission denied")
                .setMessage("Give storage access to take menu picture")
                .setPositiveButton("sure") { dialog, which ->
                    UploadActivity.generateAlertDialogAgain = false
                    dialog.cancel()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(permissionsArray, STORAGE_PERMISSION_REQUEST_ID)
                    }
                }
                .setOnDismissListener {
                    if (UploadActivity.generateAlertDialogAgain) {
                        builder.show()
                    }
                }
                .show()
    }

    override fun onResume() {
        super.onResume()
        UploadActivity.generateAlertDialogAgain = true;
    }
}





