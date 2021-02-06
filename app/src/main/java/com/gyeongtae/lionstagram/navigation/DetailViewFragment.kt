package com.gyeongtae.lionstagram.navigation

import android.os.Binder
import android.os.Bundle
import android.renderscript.ScriptGroup
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gyeongtae.lionstagram.R
import com.gyeongtae.lionstagram.databinding.ActivityMainBinding
import com.gyeongtae.lionstagram.databinding.FragmentDetailBinding
import com.gyeongtae.lionstagram.databinding.ItemDetailBinding
import com.gyeongtae.lionstagram.navigation.model.ContentDTO

class DetailViewFragment : Fragment() {
    // 뷰가 사라질 때, 즉 메모리에서 날라갈 때 같이 날리기 위해 따로 빼두기
    private var fragmentDetailBinding: FragmentDetailBinding? = null
    var firestore: FirebaseFirestore? = null
    var uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDetailBinding.inflate(inflater, container, false)
        fragmentDetailBinding = binding

        firestore = FirebaseFirestore.getInstance()
        binding.detailviewfragmentRv.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRv.layoutManager = LinearLayoutManager(activity)
        return fragmentDetailBinding!!.root
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(var viewBinding: ItemDetailBinding) :
            RecyclerView.ViewHolder(viewBinding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var customViewHolder = holder as CustomViewHolder

            //UserId
            customViewHolder.viewBinding.detailviewitemTvProfileText.text =
                contentDTOs!![position].userId

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUri)
                .into(customViewHolder.viewBinding.detailviewitemIvContent)

            //Explain of content
            customViewHolder.viewBinding.detailviewitemTvExplain.text =
                contentDTOs!![position].explain

            //likes
            customViewHolder.viewBinding.detailviewitemTvFavoritecounter.text =
                "Likes" + contentDTOs!![position].favoriteCount

            //Profile Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUri)
                .into(customViewHolder.viewBinding.detailviewitemIvProfileImg)

            //버튼이 클릭되었을 때
            customViewHolder.viewBinding.detailviewitemIvFavorite.setOnClickListener {
                favoriteEvent(position)
            }

            if (contentDTOs!![position].favorites.containsKey(uid)) {
                //버튼이 클릭된 상태일 때
                customViewHolder.viewBinding.detailviewitemIvFavorite.setImageResource(R.drawable.ic_favorite)
            } else {
                //버튼이 클릭되지 않은 상태일 떄
                customViewHolder.viewBinding.detailviewitemIvFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        fun favoriteEvent(position: Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    //버튼이 클릭되었을 때
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)
                } else {
                    //버튼이 클릭되지 않았을 떄
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO?.favorites[uid!!] = true
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
    }

    override fun onDestroyView() {
        fragmentDetailBinding = null
        super.onDestroyView()
    }
}