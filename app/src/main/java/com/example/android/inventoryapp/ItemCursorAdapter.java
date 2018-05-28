package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * {@link ItemCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of item data as its data source. This adapter knows
 * how to create list items for each row of item data in the {@link Cursor}.
 */
public class ItemCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ItemCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView productNameTextView = view.findViewById(R.id.product_name);
        TextView priceTextView = view.findViewById(R.id.price);
        TextView quantityTextView = view.findViewById(R.id.quantity);
        Button soldButton = view.findViewById(R.id.soldButton);

        // Find the columns of item attributes that we're interested in
        int productNameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE);
        final int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);

        // Read the item attributes from the Cursor for the current item
        String productName = cursor.getString(productNameColumnIndex);
        String priceColumn = cursor.getString(priceColumnIndex);
        String quantityColumn = cursor.getString(quantityColumnIndex);

        // Add text to display values
        String itemPriceText = "Price: $" + priceColumn;
        String itemQuantityText = "Quantity: " + quantityColumn;

        // Update the TextViews with the attributes for the current item
        productNameTextView.setText(productName);
        priceTextView.setText(itemPriceText);
        quantityTextView.setText(itemQuantityText);

        String currentQuantity = cursor.getString(quantityColumnIndex);
        final int quantityIntCurrent = Integer.valueOf(currentQuantity);
        final int productId = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));

        soldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (quantityIntCurrent > 0) {
                    int newQuantity = quantityIntCurrent - 1;
                    Uri quantityUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, productId);

                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, newQuantity);
                    context.getContentResolver().update(quantityUri, values, null, null);
                } else {
                    Toast.makeText(context, "This item is out of stock!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}