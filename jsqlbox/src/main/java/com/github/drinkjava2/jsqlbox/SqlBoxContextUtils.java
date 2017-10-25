/*
 * Copyright (C) 2016 Yong Zhu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jsqlbox;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayHandler;

import com.github.drinkjava2.jdialects.Dialect;
import com.github.drinkjava2.jdialects.ModelUtils;
import com.github.drinkjava2.jdialects.StrUtils;
import com.github.drinkjava2.jdialects.Type;
import com.github.drinkjava2.jdialects.annotation.jpa.GenerationType;
import com.github.drinkjava2.jdialects.id.IdGenerator;
import com.github.drinkjava2.jdialects.id.IdentityIdGenerator;
import com.github.drinkjava2.jdialects.model.ColumnModel;
import com.github.drinkjava2.jdialects.model.TableModel;

/**
 * SqlBoxContextUtils is utility class store static methods about SqlBoxContext
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public abstract class SqlBoxContextUtils {
	/**
	 * Read database Meta info into SqlBox[]
	 */
	public static SqlBox[] metaDataToModels(SqlBoxContext ctx, Dialect dialect) {
		Connection con = null;
		SQLException sqlException = null;
		SqlBox[] sqlBoxes = null;
		try {
			con = ctx.prepareConnection();
			TableModel[] tableModels = ModelUtils.db2Model(con, dialect);
			sqlBoxes = new SqlBox[tableModels.length];
			for (int i = 0; i < tableModels.length; i++) {
				SqlBox box = new SqlBox();
				box.setContext(ctx);
				box.setTableModel(tableModels[i]);
				sqlBoxes[i] = box;
			}
		} catch (SQLException e) {
			sqlException = e;
		} finally {
			try {
				ctx.close(con);
			} catch (SQLException e) {
				if (sqlException != null)
					sqlException.setNextException(e);
				else
					sqlException = e;
			}
		}
		if (sqlException != null)
			throw new SqlBoxException(sqlException);
		return sqlBoxes;
	}

	private static ColumnModel findMatchColumnForJavaField(String entityField, SqlBox box) {
		ColumnModel col = findMatchColumnForJavaField(entityField, box.getTableModel());
		if (col == null) {
			String tableName = box.getTableModel().getTableName();
			TableModel metaTableModel = box.getContext().getMetaTableModel(tableName);
			col = findMatchColumnForJavaField(entityField, metaTableModel);
		}
		if (col == null)
			throw new SqlBoxException("Can not find database column match entity field '" + entityField + "'");
		return col;
	}

	private static ColumnModel findMatchColumnForJavaField(String entityField, TableModel tableModel) {
		if (tableModel == null)
			return null;
		List<ColumnModel> columns = tableModel.getColumns();
		ColumnModel result = null;
		String underLineFieldName = SqlBoxStrUtils.camelToLowerCaseUnderline(entityField);
		for (ColumnModel col : columns) {
			if (entityField.equalsIgnoreCase(col.getEntityField())
					|| underLineFieldName.equalsIgnoreCase(col.getColumnName())) {
				if (result != null)
					throw new SqlBoxException("Field '" + entityField + "' found duplicated columns definition");
				result = col;
			}
		}
		return result;
	}

	/**
	 * Insert entityBean into database, and change ID fields to values generated
	 * by IdGenerator (identity or sequence or UUID...)
	 */
	public static void insert(SqlBoxContext ctx, Object entityBean) {
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tableModel.getTableName()).append(" (");

		List<Object> params = new ArrayList<Object>();
		String identityFieldName = null;
		Type identityType = null;
		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());

		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getInsertable()) {
				if (col.getIdGenerationType() != null || !StrUtils.isEmpty(col.getIdGeneratorName())) {
					IdGenerator idGen = col.getIdGenerator();
					if (GenerationType.IDENTITY.equals(idGen.getGenerationType())) {
						if (identityFieldName != null)
							throw new SqlBoxException(
									"More than 1 identity field found for model '" + tableModel.getTableName() + "'");
						identityFieldName = fieldName;
					} else {
						sb.append(col.getColumnName()).append(", ");
						Object id = idGen.getNextID(ctx, ctx.getDialect(), col.getColumnType());
						params.add(id);
						writeValueToBeanField(entityBean, fieldName, id);
					}
				} else {
					Object value = readValueFromBeanField(entityBean, fieldName);
					sb.append(col.getColumnName()).append(", ");
					params.add(value);
				}
			}
		}
		if (!params.isEmpty())
			sb.setLength(sb.length() - 2);// delete the last ", " character
		sb.append(") values(").append(SqlBoxStrUtils.getQuestionsStr(params.size())).append(")");
		int result = ctx.nExecute(sb.toString(), params.toArray(new Object[params.size()]));
		if (ctx.isBatchEnabled())
			return;
		if (result != 1)
			throw new SqlBoxException(result + " row record be inserted.");
		if (identityFieldName != null) {// write identity id to Bean field
			Object identityId = IdentityIdGenerator.INSTANCE.getNextID(ctx, ctx.getDialect(), identityType);
			writeValueToBeanField(entityBean, identityFieldName, identityId);
		}
	}

	/** Update entityBean according primary key */
	public static int update(SqlBoxContext ctx, Object entityBean) {
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();

		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(tableModel.getTableName()).append(" set ");

		List<Object> normalParams = new ArrayList<Object>();
		List<Object> pkeyParams = new ArrayList<Object>();
		List<ColumnModel> pkeyColumns = new ArrayList<ColumnModel>();

		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());
		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getUpdatable()) {
				Object value = readValueFromBeanField(entityBean, fieldName);
				if (!col.getPkey()) {
					normalParams.add(value);
					sb.append(col.getColumnName()).append("=?, ");
				} else {
					pkeyParams.add(value);
					pkeyColumns.add(col);
				}
			}
		}
		if (!normalParams.isEmpty())
			sb.setLength(sb.length() - 2);// delete the last ", " characters
		if (pkeyColumns.isEmpty())
			throw new SqlBoxException("No primary column setting found for entityBean");
		sb.append(" where ");
		for (ColumnModel col : pkeyColumns)
			sb.append(col.getColumnName()).append("=? and ");
		sb.setLength(sb.length() - 5);// delete the last " and " characters
		for (Object pkeyParam : pkeyParams)
			normalParams.add(pkeyParam);// join PKey values
		return box.context.nUpdate(sb.toString(), normalParams.toArray(new Object[normalParams.size()]));
	}

	/**
	 * Delete entityBean in database according primary key value
	 */
	public static void delete(SqlBoxContext ctx, Object entityBean) {
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();

		List<Object> pkeyParameters = new ArrayList<Object>();
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(tableModel.getTableName()).append(" where ");
		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());
		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getPkey()) {
				Object value = readValueFromBeanField(entityBean, fieldName);
				sb.append(col.getColumnName()).append("=?, ");
				pkeyParameters.add(value);
			}
		}
		sb.setLength(sb.length() - 2);// delete the last "," character
		if (pkeyParameters.isEmpty())
			throw new SqlBoxException("No primary key set for entityBean");
		int rowAffected = box.context.nExecute(sb.toString(),
				pkeyParameters.toArray(new Object[pkeyParameters.size()]));
		if (ctx.isBatchEnabled())
			return;
		if (rowAffected <= 0)
			throw new SqlBoxException("No row be deleted for entityBean");
		if (rowAffected > 1)
			throw new SqlBoxException("Multiple rows affected when delete entityBean");
	}

	@SuppressWarnings("unchecked")
	public static <T> T load(SqlBoxContext ctx, Class<?> entityClass, Object pkeyValue) {
		SqlBoxException.assureNotNull(entityClass, "entityClass can not be null");
		SqlBoxException.assureNotNull(entityClass, "pkey can not be null");

		Map<String, Object> pkValueMap = null;
		if (pkeyValue instanceof Map)
			pkValueMap = (Map<String, Object>) pkeyValue;// NOSONAR
		else {
			pkValueMap = new HashMap<String, Object>();
			pkValueMap.put("ooxxooxx", pkeyValue);
		}

		Object entity = null;
		try {
			entity = entityClass.newInstance();
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}

		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entity);
		TableModel model = box.getTableModel();

		StringBuilder sb = new StringBuilder("select ");
		List<Object> pkParams = new ArrayList<Object>();

		List<ColumnModel> pkeyColumns = new ArrayList<ColumnModel>();
		List<ColumnModel> allColumns = new ArrayList<ColumnModel>();
		List<String> allFieldNames = new ArrayList<String>();

		Map<String, Method> writeMethods = ClassCacheUtils.getClassWriteMethods(entityClass);

		for (String fieldName : writeMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable()) {
				allColumns.add(col);
				allFieldNames.add(fieldName);
				sb.append(col.getColumnName()).append(", ");
				if (col.getPkey()) {
					pkeyColumns.add(col);
					if (pkValueMap.size() == 1)
						pkParams.add(pkValueMap.entrySet().iterator().next().getValue());
					else
						pkParams.add(pkValueMap.get(fieldName));
				}
			}
		}
		if (pkeyColumns.isEmpty())
			throw new SqlBoxException("No primary key set for entityBean");
		sb.setLength(sb.length() - 2);// delete the last ", "
		if (pkParams.size() != pkValueMap.size())
			throw new SqlBoxException("Wrong number of primary key parameters: expected " + pkParams.size()
					+ ", was given " + pkValueMap.size());

		sb.append(" from ").append(model.getTableName()).append(" where ");
		for (ColumnModel col : pkeyColumns)
			sb.append(col.getColumnName()).append("=? and ");
		sb.setLength(sb.length() - 5);// delete the last " and "

		try {
			Object[] values = ctx.nQuery(new ArrayHandler(), sb.toString(),
					pkParams.toArray(new Object[pkParams.size()]));
			for (int i = 0; i < values.length; i++) {
				Method writeMethod = writeMethods.get(allFieldNames.get(i));
				writeMethod.invoke(entity, values[i]);
			}
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}
		return (T) entity;
	}

	/** Read value from entityBean field */
	private static Object readValueFromBeanField(Object entityBean, String fieldName) {
		Method readMethod = ClassCacheUtils.getClassFieldReadMethod(entityBean.getClass(), fieldName);
		if (readMethod == null)
			throw new SqlBoxException("Can not find Java bean read method for column '" + fieldName + "'");
		try {
			return readMethod.invoke(entityBean);
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}
	}

	/** write value to entityBean field */
	private static void writeValueToBeanField(Object entityBean, String fieldName, Object value) {
		Method writeMethod = ClassCacheUtils.getClassFieldWriteMethod(entityBean.getClass(), fieldName);
		if (writeMethod == null)
			throw new SqlBoxException("Can not find Java bean read method for column '" + fieldName + "'");
		try {
			writeMethod.invoke(entityBean, value);
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}
	}

	private static void checkBeanAndBoxExist(Object entityBean, SqlBox box) {
		SqlBoxException.assureNotNull(entityBean, "Assert error, entityBean can not be null");
		SqlBoxException.assureNotNull(box, "Assert error, box of entityBean can not be null");
		SqlBoxException.assureNotNull(box.getContext(), "Assert error, box's SqlBoxContext can not be null");
		SqlBoxException.assureNotNull(box.getTableModel(), "Assert error, box's TableModel can not be null");
		SqlBoxException.assureNotEmpty(box.getTableModel().getTableName(),
				"Assert error, box's tableName can not be null");
	}

}