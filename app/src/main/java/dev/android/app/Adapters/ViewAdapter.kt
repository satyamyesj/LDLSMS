package dev.android.app.Adapters

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.android.app.R


import android.support.v7.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import dev.android.app.BusinessLocatorActivity

import kotlinx.android.synthetic.main.card_view.view.*

class ViewAdapter(val context: Context, val count: Int) : RecyclerView.Adapter<ViewAdapter.myViewHolder>() {
    var uid: String? = null
    var firebaseAuth = FirebaseAuth.getInstance()
    var ownersReference = FirebaseDatabase.getInstance().getReference("owners")
    var uidsReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("uids")


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.card_view, parent, false)
        return myViewHolder(view)

    }

    override fun getItemCount(): Int {
        return count
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        val pos = position.toString()
        var progressDialog:ProgressDialog;
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("fetching menus...")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show();
        uidsReference.child(pos).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented")
            }
            override fun onDataChange(p0: DataSnapshot) {
                uid = p0.value.toString()
                holder.setData(uid!!)
            }
        })
        progressDialog.dismiss()
    }

    inner class myViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener({
                var intent=Intent(context, BusinessLocatorActivity::class.java)
                intent.putExtra("uid",uid);
                context.startActivity(intent)
            })
        }
        fun setData(uid: String) {
           ownersReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented")
                }
                override fun onDataChange(p0: DataSnapshot) {
                    val url=p0.child("latest_menu_url").value.toString().trim();

                    if(!url.isEmpty()) {
                        Picasso.get().load(url).into(itemView.imageView)
                    }
                    itemView.business_title.text=p0.child("business_title").value.toString()
                    itemView.menu_descriptionTextView.text=p0.child("menu_description_text").value.toString()
                }
            })
        }
    }
}