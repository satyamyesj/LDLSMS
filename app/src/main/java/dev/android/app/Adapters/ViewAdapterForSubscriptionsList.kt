package dev.android.app.Adapters

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.android.app.R


import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import dev.android.app.BusinessLocatorActivity

import kotlinx.android.synthetic.main.card_view.view.*
import kotlinx.android.synthetic.main.card_view_for_subscriptions.view.*

class ViewAdapterForSubscriptionsList(val context: Context, val count: Int) : RecyclerView.Adapter<ViewAdapterForSubscriptionsList.myViewHolder>() {
    var uid: String? = null
    var interator=-1
    var firebaseAuth = FirebaseAuth.getInstance()
    var ownersReference = FirebaseDatabase.getInstance().getReference("owners")
    var uidsReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("uids")


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.card_view_for_subscriptions, parent, false)
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
        interator=position+1
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
              val marker=Marker(context,uid,interator.toString())
                marker.showMarkEntry();
            })
        }
        fun setData(uid: String) {
           ownersReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented")
                }
                override fun onDataChange(p0: DataSnapshot) {
                   var iteratorString:String= interator.toString()
                    itemView.subscriberName.text= p0.child("subscriptions").child(iteratorString).child("name").value.toString()
                    itemView.subscriberPhone.text=p0.child("subscriptions").child(iteratorString).child("phone_number").value.toString()
                    itemView.subscriptionDetails.text="Date: "+p0.child("subscriptions").child(iteratorString).child("date_of_subscription").value.toString()+
                                                                              "  Days: "+p0.child("subscriptions").child(iteratorString).child("no_of_days").value.toString()+
                                                                               "  Coupans: "+  p0.child("subscriptions").child(iteratorString).child("no_of_coupans").value.toString()
                }
            })
        }
    }
}