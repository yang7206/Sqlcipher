package com.j256.ormlite.android.apptools;

import static com.j256.ormlite.stmt.StatementBuilder.StatementType.SELECT;

import java.sql.SQLException;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;

import com.j256.ormlite.android.AndroidCompiledStatement;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.DaoObserver;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Cursor loader supported by later Android APIs that allows asynchronous content loading.
 * 
 * @author emmby
 */
public class OrmLiteCursorLoader<T> extends AsyncTaskLoader<Cursor> implements DaoObserver {

	protected Dao<T, ?> dao;
	protected PreparedQuery<T> query;
	protected Cursor cursor;

	public OrmLiteCursorLoader(Context context, Dao<T, ?> dao, PreparedQuery<T> query) {
		super(context);
		this.dao = dao;
		this.query = query;
		dao.registerObserver(this);
	}

	@Override
	public Cursor loadInBackground() {
		Cursor cursor;
		try {
			DatabaseConnection connection = dao.getConnectionSource().getReadOnlyConnection();
			cursor = ((AndroidCompiledStatement) query.compile(connection, SELECT)).getCursor();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		// fill the cursor with results
		cursor.getCount();
		return cursor;
	}

	@Override
	public void deliverResult(Cursor newCursor) {
		if (isReset()) {
			// an async query came in while the loader is stopped
			if (newCursor != null) {
				newCursor.close();
			}
			return;
		}

		Cursor oldCursor = cursor;
		cursor = newCursor;

		if (isStarted()) {
			super.deliverResult(newCursor);
		}

		// close the old cursor if necessary
		if (oldCursor != null && oldCursor != newCursor && !oldCursor.isClosed()) {
			oldCursor.close();
		}
	}

	@Override
	protected void onStartLoading() {
		if (cursor == null) {
			forceLoad();
		} else {
			deliverResult(cursor);
			if (takeContentChanged()) {
				forceLoad();
			}
		}
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

	@Override
	public void onCanceled(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		if (cursor != null) {
			if (!cursor.isClosed()) {
				cursor.close();
			}
			cursor = null;
		}
	}

	public void onChange() {
		onContentChanged();
	}

	public PreparedQuery<T> getQuery() {
		return query;
	}

	public void setQuery(PreparedQuery<T> mQuery) {
		this.query = mQuery;
	}
}
