package com.threeplay.android.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by eliranbe on 1/12/17.
 */
public class InteractionAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    private final RecyclerView.Adapter<T> adapter;
    private RecyclerView recyclerView;
    private OnItemSelectedListener<T> itemSelectedListener;
    private int currentSelectedPosition = 0;
    private RecyclerView.ViewHolder currentSelectedHolder = null;
    private final RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            while ( itemCount-- > 0 ) {
                notifyItemMoved(fromPosition++, toPosition++);
            }
        }
    };


    public static <T extends RecyclerView.ViewHolder> InteractionAdapter<T> wrap(RecyclerView.Adapter<T> adapter, OnItemSelectedListener<T> listener){
        InteractionAdapter<T> interactionAdapter = new InteractionAdapter<>(adapter);
        interactionAdapter.setOnItemSelectedListener(listener);
        return interactionAdapter;
    }

    public RecyclerView.Adapter<T> getAdapter() {
        return adapter;
    }

    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public InteractionAdapter(RecyclerView.Adapter<T> adapter) {
        this.adapter = adapter;
        adapter.registerAdapterDataObserver(dataObserver);
    }

    @Override
    protected void finalize() throws Throwable {
        this.adapter.unregisterAdapterDataObserver(dataObserver);
        super.finalize();
    }


    public void setOnItemSelectedListener(OnItemSelectedListener<T> itemSelectedListener){
        this.itemSelectedListener = itemSelectedListener;
    }

    public void setSelectedItem(int index){
        if ( currentSelectedHolder != null ) { currentSelectedHolder.itemView.setSelected(false); }
        currentSelectedPosition = index;
        if ( recyclerView != null && currentSelectedPosition != -1 ) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(currentSelectedPosition);
            if ( holder != null ) {
                currentSelectedHolder = holder;
                holder.itemView.setSelected(true);
            }
        }
    }

    @Override
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        final T holder = adapter.onCreateViewHolder(parent, viewType);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( itemSelectedListener != null ) {
                    int position = holder.getAdapterPosition();
                    setSelectedItem(position);
                    itemSelectedListener.onItemSelected(InteractionAdapter.this, position);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(T holder, int position) {
        adapter.onBindViewHolder(holder, position);
        boolean selected = currentSelectedPosition == position;
        holder.itemView.setSelected(selected);
        if ( selected ) { currentSelectedHolder = holder; }
    }

    @Override
    public int getItemCount() {
        return adapter.getItemCount();
    }

    public interface OnItemSelectedListener<T extends RecyclerView.ViewHolder> {
        void onItemSelected(RecyclerView.Adapter<T> adapter, int position);
    }

}
