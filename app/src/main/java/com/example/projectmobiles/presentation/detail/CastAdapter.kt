package com.example.projectmobiles.presentation.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmobiles.data.model.Cast
import com.example.projectmobiles.databinding.ViewholderActorsBinding

class CastAdapter(private val castList: List<Cast>) :
    RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    inner class CastViewHolder(val binding: ViewholderActorsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = ViewholderActorsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val cast = castList[position]
        holder.binding.nameTxt.text = cast.name

        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w185${cast.profile_path}")
            .into(holder.binding.itemImage)
    }

    override fun getItemCount(): Int = castList.size
}

