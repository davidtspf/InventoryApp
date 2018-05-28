package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.support.v4.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * Displays list of items that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ITEM_LOADER = 0;

    private ItemCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the items data
        ListView itemListView = findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of pet data in the Cursor.
        // There is no pet data until the loader finishes, so pass in null for the Cursor.
        mCursorAdapter = new ItemCursorAdapter(this, null);
        itemListView.setAdapter(mCursorAdapter);

        // Setup item click listener
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //  Find the item that was clicked
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                Uri currentItemUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                intent.setData(currentItemUri);
                startActivity(intent);
            }
        });

        // Kick off the loader
        getSupportLoaderManager().initLoader(ITEM_LOADER, null, this);
    }

    /**
     * Helper method to insert hardcoded item data into the database. For debugging purposes only.
     */
    private void insertItem() {
        // Create a ContentValues object where column names are the keys,
        // and the item's attributes are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, "Widget");
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, 5);
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, 3);
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_NAME, "Widgets Supplier");
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_PHONE_NUMBER, "212-555-5555");

        // Insert a new row for the item into the provider using the ContentResolver.
        // Use the {@link InventoryEntry#CONTENT_URI} to indicate that we want to insert
        // into the items database table.
        // Receive the new content URI that will allow us to access the item's data
        // in the future.
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertItem();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllItems();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY };

        return new CursorLoader(this,
                InventoryEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Helper method to delete all items in the database.
     */
    private void deleteAllItems() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from items database");
    }


}
