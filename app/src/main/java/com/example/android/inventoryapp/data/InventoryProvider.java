package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /** Database helper object */
    private InventoryDbHelper mDbHelper;

    /** URI matcher code for the content URI for the items table */
    private static final int ITEMS = 100;

    /** URI matcher code for the content URI for a single item in the items table */
    private static final int ITEM_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS, ITEMS);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS + "/#", ITEM_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // For the ITEMS code, query the inventory table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the inventory table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case ITEM_ID:
                // For the ITEM_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.inventoryapp/items/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the inventory table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the cursor so we know what content
        // URI the Cursor was created for. If the data at this URI changes
        // then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertItem(Uri uri, ContentValues values) {
        // Check that the product name is not null
        String productName = values.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);
        if (productName == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        // Check that the product price is not null
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Item requires a valid price");
        }

        // Check that the product quantity is not null
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Item requires a valid quantity");
        }

        // Check that the supplier's name is not null
        String supplierName = values.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Item requires a supplier");
        }

        // Check that the supplier's phone number is not null
        String supplierPhoneNumber = values.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_PHONE_NUMBER);
        if (supplierPhoneNumber == null) {
            throw new IllegalArgumentException("Item requires supplier's phone number");
        }

        // Get writable data base
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new item with the given values
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the item content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ITEM_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                getContext().getContentResolver().notifyChange(uri, null);
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                // For the ITEM_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update items in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more items).
     * Return the number of rows that were successfully updated.
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Check that the product name is not null
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_NAME)) {
            String productName = values.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);
            if (productName == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        // Check that the product price is not null
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_PRICE)) {
            Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Item requires a valid price");
            }
        }

        // Check that the product quantity is not null
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Item requires a valid quantity");
            }
        }

        // Check that the supplier's name is not null
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_NAME)) {
            String supplierName = values.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_NAME);
            if (supplierName == null) {
                throw new IllegalArgumentException("Item requires a supplier");
            }
        }

        // Check that the supplier's phone number is not null
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_PHONE_NUMBER)) {
            String supplierPhoneNumber = values.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER_PHONE_NUMBER);
            if (supplierPhoneNumber == null) {
                throw new IllegalArgumentException("Item requires supplier's phone number");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Get writable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = db.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

}
