package database;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

public class DBApp {
	public static File file;

	public static transient Vector<String> tableNames;

	public DBApp() throws IOException {
		this.init();

		tableNames = new Vector<String>();
	}

	public void init() // this does whatever initialization you would like

	{
		file = new File("metadata.csv");
		File config = new File("src/resources");
		boolean bool = config.mkdir();
		if (bool) {
			System.out.println("Directory created successfully");
		} else {
			System.out.println("Sorry couldn’t create specified directory");
		}
		try {
			File newconfig = new File("src/resources/DBApp.config");
			newconfig.createNewFile();
			Properties prop1 = new Properties();
			prop1.setProperty("MaximumRowsCountinTablePage", "2");
			Properties prop2 = new Properties();
			prop2.setProperty("MaximumEntriesinOctreeNode", "16");
			FileWriter writer = new FileWriter(newconfig);
			prop1.store(writer, null);
			prop2.store(writer, null);
			writer.close();
		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	// or leave it empty if there is no code you want to
	// execute at application startup
	// following method creates one table only
	// strClusteringKeyColumn is the name of the column that will be the primary key
	// and the clustering column as well.
	// The data type of that column will be passed in htblColNameType
	// htblColNameValue will have the column name as key and the data
	// type as value
	// htblColNameMin and htblColNameMax for passing minimum and maximum values
	// for data in the column. Key is the name of the column
	public void createTable(String strTableName, String strClusteringKeyColumn,
			Hashtable<String, String> htblColNameType, Hashtable<String, String> htblColNameMin,
			Hashtable<String, String> htblColNameMax) throws DBAppException {
		if (tableNames.contains(strTableName)) {
			System.out.println("table already exists");

		} else {
			Set<String> x = htblColNameType.keySet();
			Set<String> max = htblColNameMax.keySet();
			for (String max1 : max) {
				if (!x.contains(max1)) {
					throw new DBAppException("max columns do not match table columns");
				}

			}
			Set<String> min = htblColNameMax.keySet();
			for (String min1 : min) {
				if (!x.contains(min1)) {
					throw new DBAppException("min columns do not match table columns");
				}

			}
			Table t;
			try {
				t = new Table(strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin, htblColNameMax,
						file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

			tableNames.add(strTableName);
			deserializeTable(strTableName);
			t.columnNum = htblColNameType.size();
			if (t.columnNum != htblColNameMax.size() || t.columnNum != htblColNameMin.size())
				throw new DBAppException("maximum or minimum values missing");

			t.columnNum = htblColNameType.size();
			Set<String> names = htblColNameType.keySet();
			Iterator<String> i = names.iterator();
			while (i.hasNext()) {
				t.columnNames.add(i.next());
			}

			serializeTable(strTableName, t);
			t = null;
			System.gc();

		}
	}

	// following method creates an octree
	// depending on the count of column names passed.
	// If three column names are passed, create an octree.
	// If only one or two column names is passed, throw an Exception.
	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException {
		if (tableNames.isEmpty()) {
			throw new DBAppException("No tables to create index on");
		}
		if (!tableNames.contains(strTableName)) {
			throw new DBAppException("No table with that name");
		}
		if (strarrColName.length != 3) {
			throw new DBAppException("cannot create index on less or more than 3 columns");
		}
		Table needed = deserializeTable(strTableName);

		for (int i = 0; i < strarrColName.length; i++) {
			if (!needed.columnNames.contains(strarrColName[i])) {
				throw new DBAppException("No column with name " + strarrColName[i] + " in your table");
			}
			try {
				if (alreadyHasIndex(strTableName, strarrColName[i], file)) {
					throw new DBAppException("Column " + strarrColName[i] + " already has an index in your table");
				}
			} catch (IOException | DBAppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}

		String[][] indexNameMinMax = new String[4][3];
		indexNameMinMax[0] = strarrColName;

		try {
			indexNameMinMax = gettingMinMax(strTableName, indexNameMinMax, file);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		needed.indicies.add(indexNameMinMax);

		Object[][] indexObject = (Object[][]) indexNameMinMax;


		Boundary bbBoundry = null;
		try {
			bbBoundry = createBoundry(indexObject);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Octree octree = null;
		try {
			octree = new Octree(0, bbBoundry);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			writetoMetaData(strTableName, strarrColName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!needed.empty) {
			for (int i = 0; i < needed.pages.size(); i++) {
				Page p = deserializePage(strTableName, i);
				for (int j = 0; j < p.rows.size(); j++) {
					Hashtable<String, Object> current = p.rows.get(j);
					Object x = current.get(strarrColName[0]);
					Object y = current.get(strarrColName[1]);
					Object z = current.get(strarrColName[2]);
					int reference = i;

					try {
						octree.insert(x, y, z, reference);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		}
		serializeOctree(strTableName, strarrColName, octree);
		serializeTable(strTableName, needed);
		octree = null;
		needed = null;
		System.gc();

	}

	public Boundary createBoundry(Object[][] indexObject) throws ParseException {
		// TODO Auto-generated method stub
		Object xMax = null, xMin = null, yMax = null, yMin = null, zMax = null, zMin = null;
		for (int i = 0; i < 3; i++) {
			switch ((String) indexObject[3][i]) {
			case "java.lang.String":
				if (i == 0) {
					xMax = indexObject[2][i];
					xMin = indexObject[1][i];
				} else if (i == 1) {
					yMax = indexObject[2][i];
					yMin = indexObject[1][i];
				} else {
					zMax = indexObject[2][i];
					zMin = indexObject[1][i];
				}
				break;

			case "java.lang.Integer":

				if (i == 0) {
					xMax = Integer.parseInt((String) indexObject[2][i]);
					xMin = Integer.parseInt((String) indexObject[1][i]);
				} else if (i == 1) {
					yMax = Integer.parseInt((String) indexObject[2][i]);
					yMin = Integer.parseInt((String) indexObject[1][i]);
				} else {
					zMax = Integer.parseInt((String) indexObject[2][i]);
					zMin = Integer.parseInt((String) indexObject[1][i]);
				}
				break;
			case "java.lang.Double":

				if (i == 0) {
					xMax = Double.parseDouble((String) indexObject[2][i]);
					xMin = Double.parseDouble((String) indexObject[1][i]);
				} else if (i == 1) {
					yMax = Double.parseDouble((String) indexObject[2][i]);
					yMin = Double.parseDouble((String) indexObject[1][i]);
				} else {
					zMax = Double.parseDouble((String) indexObject[2][i]);
					zMin = Double.parseDouble((String) indexObject[1][i]);
				}
				break;
			case "java.util.Date":
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH);
				if (i == 0) {
					xMax = formatter.parse((String) indexObject[2][i]);
					xMin = formatter.parse((String) indexObject[1][i]);
				} else if (i == 1) {
					yMax = formatter.parse((String) indexObject[2][i]);
					yMin = formatter.parse((String) indexObject[1][i]);
				} else {
					zMax = formatter.parse((String) indexObject[2][i]);
					zMin = formatter.parse((String) indexObject[1][i]);
				}
				break;
			
			}
		}
		return new Boundary(xMin, yMin, zMin, xMax, yMax, zMax);
	}

	public void writetoMetaData(String strTableName, String[] strarrColName) throws IOException, DBAppException {
		
		String tempFile = "temp.csv";
		File newfile = new File(tempFile);
		String tablename = "";
		String columnname = "";
		String columntype = "";
		String clusterkey = "";
		String indexname = "";
		String indextype = "";
		String min = "";
		String max = "";
		String idname = "";
		ArrayList<String> ss = new ArrayList<String>();
		for (String s : strarrColName) {
			idname += s;
			ss.add(s);
		}

		try {

			FileWriter fw = new FileWriter(tempFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String temp = br.readLine();
			while (temp != null) {
				String[] s = temp.split(",");
				tablename = s[0];
				
				columnname = s[1];

				columntype = s[2];
				clusterkey = s[3];
				indexname = s[4];

				indextype = s[5];
				min = s[6];
				max = s[7];

				if (tablename.equals(strTableName)) {

					if (ss.contains(columnname)) {
						
						bw.append(tablename + "," + columnname + "," + columntype + "," + clusterkey + "," + idname
								+ ",octree," + min + "," + max);
						bw.newLine();

					} else {
						bw.append(tablename + "," + columnname + "," + columntype + "," + clusterkey + "," + indexname
								+ "," + indextype + "," + min + "," + max);
						bw.newLine();

					}

					temp = br.readLine();
				} else {
					bw.append(tablename + "," + columnname + "," + columntype + "," + clusterkey + "," + indexname + ","
							+ indextype + "," + min + "," + max);
					bw.newLine();
					temp = br.readLine();

				}

			}
			bw.close();
			br.close();
			file.delete();
			File dump = new File("metadata.csv");
			newfile.renameTo(dump);
		} catch (Exception e) {
			throw new DBAppException("ERROR");
		}
	}

	public String[][] gettingMinMax(String strTableName, String[][] indexNameMinMax, File filePath) throws IOException {
		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		String temp = br.readLine();
		while (temp != null) {
			for (int i = 0; i < 3; i++) {

				String columnName = indexNameMinMax[0][i];

				String[] splitted = temp.split(","); // from metadata

				if (strTableName.equals(splitted[0])) { // same table name

					if (columnName.equals(splitted[1])) { // same column name

						indexNameMinMax[1][i] = splitted[6];

						indexNameMinMax[2][i] = splitted[7];

						indexNameMinMax[3][i] = splitted[2];

					}

				}
			}
			temp = br.readLine();

		}
		br.close();

		return indexNameMinMax;
	}

	public boolean alreadyHasIndex(String strTableName, String columnName, File filePath) throws IOException {

		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		String temp = br.readLine();

		while (temp != null) {

			String[] splitted = temp.split(","); // from metadata

			// Table Name, Column Name, Column Type, ClusteringKey, IndexName,IndexType,
			// min, max
			if (strTableName.equals(splitted[0])) { // same table name

				if (columnName.equals(splitted[1])) { // same column name

					if (!splitted[4].equals("null")) {

						br.close();
						return true;
					} else {
						br.close();
						return false;
					}
				}
				temp = br.readLine();
			}

			temp = br.readLine();

		}
		br.close();
		return false;
	}

	// following method inserts one row only.
	// htblColNameValue must include a value for the primary key
	public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		// if no tables are available
		if (tableNames.isEmpty()) {
			throw new DBAppException("There are no tables to insert into, Please create a table first");// added this
																										// here
		} else {
			// find table u want to insert in
			if (!tableNames.contains(strTableName)) {
				throw new DBAppException("table doesnt exist");
			}
			Table neededTable = deserializeTable(strTableName);
			Set<String> x = htblColNameValue.keySet();
			for (String y : x) {
				if (!neededTable.columnNames.contains(y)) {
					throw new DBAppException("column doesnt exist");
				}
			}
			// check for data type match
			try {
				check(strTableName, htblColNameValue, file);
			} catch (DBAppException e) {
				System.out.println(e.toString());
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			for (String name : neededTable.columnNames) {
				if (!htblColNameValue.containsKey(name)) {
					throw new DBAppException("No nulls allowed");
				}
				if (htblColNameValue.get(name).getClass().getTypeName().equals("database.Null"))
					throw new DBAppException("No nulls allowed");
			}

			if (htblColNameValue.get(neededTable.clusteringKeyName).getClass().getTypeName().equals("database.Null")) {
				serializeTable(strTableName, neededTable);
				neededTable = null;
				System.gc();
				throw new DBAppException("primary key cant be null");
			}
			// if table doesn't have pages yet
			if (neededTable.empty) {

				Page p;
				try {
					p = new Page(neededTable.pages.size(), neededTable.tableName, neededTable);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}

				String pk = neededTable.clusteringKeyName;
				Object pkValue = htblColNameValue.get(pk);
				if (pkValue == null) {
					serializeTable(strTableName, neededTable);
					neededTable = null;
					System.gc();
					throw new DBAppException("The table you're trying to insert to doesn't have a primary key");
				}
				p.rows.addElement(htblColNameValue);

				p.pk.add(pkValue);
				p.min = pkValue;
				p.max = pkValue;
				p.usedRows++;
				neededTable.pages.add(p);

				neededTable.empty = false;

				if (!neededTable.indicies.isEmpty()) { // must check for nulls

					for (int i = 0; i < neededTable.indicies.size(); i++) {
						String[] colNames = neededTable.indicies.get(i)[0];
						Octree current = deserializeOctree(strTableName, colNames);
						Object xOC = htblColNameValue.get(colNames[0]);
						Object yOC = htblColNameValue.get(colNames[1]);
						Object zOC = htblColNameValue.get(colNames[2]);

						int ref = 0;
						try {
							current.insert(xOC, yOC, zOC, 0);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						serializeOctree(strTableName, colNames, current);
					}
				}

				p.refreshPage();
				p = null;
				System.gc();

			}

			else {

				String pk = neededTable.clusteringKeyName;
				Object pkValue = htblColNameValue.get(pk);
				Page desiredPage;

				int indexNeeded = getPageI(neededTable, pkValue);

				if (indexNeeded == -1) {
					serializeTable(strTableName, neededTable);
					neededTable = null;
					System.gc();
					throw new DBAppException("duplicate primary key error");
				} else if (indexNeeded == -2) { // in first page but its full

					desiredPage = deserializePage(strTableName, 0);

					Hashtable<String, Object> last = desiredPage.rows.get(desiredPage.rows.size() - 1);
					desiredPage.pk.remove(last.get(pk));
					desiredPage.rows.remove(last);

					desiredPage.rows.addElement(htblColNameValue);
					desiredPage.pk.add(pkValue);

					if (!neededTable.indicies.isEmpty()) { // must check for nulls
						for (int i = 0; i < neededTable.indicies.size(); i++) {
							String[] colNames = neededTable.indicies.get(i)[0];
							Octree current = deserializeOctree(strTableName, colNames);
							Object xOC = htblColNameValue.get(colNames[0]);
							Object yOC = htblColNameValue.get(colNames[1]);
							Object zOC = htblColNameValue.get(colNames[2]);
							int ref = 0;
							try {
								current.insert(xOC, yOC, zOC, ref);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							serializeOctree(strTableName, colNames, current);
						}
					}
					desiredPage.refreshPage();
					desiredPage = null;
					System.gc();
					try {
						shiftPages(neededTable, 1, last);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}

				}

				else if (indexNeeded == -4) { // create new page after last page

					try {
						desiredPage = new Page(neededTable.pages.size(), neededTable.tableName, neededTable);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}

					desiredPage.rows.addElement(htblColNameValue);

					desiredPage.usedRows++;
					desiredPage.pk.add(pkValue);
					neededTable.pages.add(desiredPage);
					desiredPage.max = pkValue;
					desiredPage.min = pkValue;
					if (!neededTable.indicies.isEmpty()) { // must check for nulls
						for (int i = 0; i < neededTable.indicies.size(); i++) {
							String[] colNames = neededTable.indicies.get(i)[0];
							Octree current = deserializeOctree(strTableName, colNames);
							Object xOC = htblColNameValue.get(colNames[0]);
							Object yOC = htblColNameValue.get(colNames[1]);
							Object zOC = htblColNameValue.get(colNames[2]);
							int ref = neededTable.pages.size() - 1;
							try {
								current.insert(xOC, yOC, zOC, ref);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							serializeOctree(strTableName, colNames, current);
						}
					}
					desiredPage.refreshPage();
					desiredPage = null;
					System.gc();

				} else if (indexNeeded == -5) { // create new page after last page

					Page newPage;
					try {
						newPage = new Page(neededTable.pages.size(), neededTable.tableName, neededTable);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}

					desiredPage = deserializePage(strTableName, neededTable.pages.size() - 1);
					Hashtable<String, Object> toShift = desiredPage.rows.lastElement();
					Object pkValueofNewPage = toShift.get(pk);
					newPage.rows.add(toShift);
					newPage.pk.add(pkValueofNewPage);
					newPage.usedRows++;
					newPage.max = pkValueofNewPage;
					newPage.min = pkValueofNewPage;
					neededTable.pages.add(newPage);
					desiredPage.rows.remove(toShift);
					desiredPage.pk.remove(pkValueofNewPage);
					desiredPage.rows.addElement(htblColNameValue);
					desiredPage.pk.add(pkValue);

					if (!neededTable.indicies.isEmpty()) { // must check for nulls
						for (int i = 0; i < neededTable.indicies.size(); i++) {
							String[] colNames = neededTable.indicies.get(i)[0];
							Octree current = deserializeOctree(strTableName, colNames);
							Object xOC = htblColNameValue.get(colNames[0]);
							Object yOC = htblColNameValue.get(colNames[1]);
							Object zOC = htblColNameValue.get(colNames[2]);
							int ref = neededTable.pages.size() - 1;
							try {
								current.insert(xOC, yOC, zOC, ref);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							serializeOctree(strTableName, colNames, current);
						}
					}
					desiredPage.refreshPage();
					newPage.refreshPage();
					desiredPage = null;
					newPage = null;
					System.gc();

				} else { // index found or page where should be is full

					desiredPage = deserializePage(strTableName, indexNeeded);

					if (desiredPage.pk.contains(pkValue)) {
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("duplicate primary key error");
					}

					if (desiredPage.full == false) {
						desiredPage.rows.addElement(htblColNameValue);

						desiredPage.usedRows++;
						if (desiredPage.usedRows == desiredPage.maxrows)
							desiredPage.full = true;
						desiredPage.pk.add(pkValue);
						desiredPage.refreshPage();
						desiredPage = null;
						System.gc();

					} else {
						Hashtable<String, Object> last = desiredPage.rows.get(desiredPage.rows.size() - 1);

						desiredPage.rows.remove(last);
						desiredPage.pk.remove(last.get(pk));
						desiredPage.rows.addElement(htblColNameValue);
						desiredPage.pk.add(pkValue);

						desiredPage.refreshPage();
						desiredPage = null;
						System.gc();
						try {
							shiftPages(neededTable, indexNeeded + 1, last);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}

					}
					if (!neededTable.indicies.isEmpty()) { // must check for nulls
						for (int i = 0; i < neededTable.indicies.size(); i++) {
							String[] colNames = neededTable.indicies.get(i)[0];
							Octree current = deserializeOctree(strTableName, colNames);
							Object xOC = htblColNameValue.get(colNames[0]);
							Object yOC = htblColNameValue.get(colNames[1]);
							Object zOC = htblColNameValue.get(colNames[2]);
							int ref = indexNeeded;
							try {
								current.insert(xOC, yOC, zOC, ref);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							serializeOctree(strTableName, colNames, current);
						}
					}
				}

			}
			serializeTable(strTableName, neededTable);
			neededTable = null;
			System.gc();

		}

	}

	public int getPageI(Table table, Object pk) throws DBAppException { // -2 for 1st page, -4 for create new page
		String type = pk.getClass().getTypeName();

		if (type.equals("java.lang.Integer")) {

			int pkI = (int) pk;

			for (int j = 0; j < table.pages.size(); j++) {

				Page p = deserializePage(table.tableName, j);

				if (j == 0 && pkI < (int) p.min) {
					if (p.full == false)
						return j;
					else
						return -2;
				} else if (pkI < (int) p.max && pkI > (int) p.min) {

					return j;

				} else if (j == table.pages.size() - 1 && pkI > (int) p.max) {
					if (p.full == false)
						return j;
					else
						return -4;
				} else if (j == table.pages.size() - 1 && pkI < (int) p.min) {
					if (p.full == false)
						return j;
					else
						return -5;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.lang.String")) {
			String pkS = (String) pk;
			for (int j = 0; j < table.pages.size(); j++) {
				Page p = deserializePage(table.tableName, j);

				if (j == 0 && pkS.compareTo((String) p.min) < 0) {

					if (p.full == false)
						return j;
					else
						return -2;
				} else if (pkS.compareTo((String) p.max) < 0 && pkS.compareTo((String) p.min) > 0) {

					return j;

				} else if (j == table.pages.size() - 1 && pkS.compareTo((String) p.max) > 0) {
					if (p.full == false)
						return j;
					else
						return -4;
				} else if (j == table.pages.size() - 1 && pkS.compareTo((String) p.min) < 0) {
					if (p.full == false)
						return j;
					else
						return -5;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.lang.Double")) {
			Double pkDD = (Double) pk;
			for (int j = 0; j < table.pages.size(); j++) {
				Page p = deserializePage(table.tableName, j);

				if (j == 0 && pkDD < (Double) p.min) {
					if (p.full == false)
						return j;
					else
						return -2;
				} else if (pkDD < (Double) p.max && pkDD > (Double) p.min) {

					return j;

				} else if (j == table.pages.size() - 1 && pkDD > (Double) p.max) {
					if (p.full == false)
						return j;
					else
						return -4;
				} else if (j == table.pages.size() - 1 && pkDD < (Double) p.min) {
					if (p.full == false)
						return j;
					else
						return -5;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.util.Date")) {
			Date pkD = (Date) pk;
			for (int j = 0; j < table.pages.size(); j++) {
				Page p = deserializePage(table.tableName, j);

				if (j == 0 && pkD.before((Date) p.min)) {
					if (p.full == false)
						return j;
					else
						return -2;
				} else if (pkD.after((Date) p.max) && pkD.after((Date) p.min)) {

					return j;

				} else if (j == table.pages.size() - 1 && pkD.after((Date) p.max)) {
					if (p.full == false)
						return j;
					else
						return -4;
				} else if (j == table.pages.size() - 1 && pkD.before((Date) p.min)) {
					if (p.full == false)
						return j;
					else
						return -5;
				}
				p.refreshPage();
				p = null;
				System.gc();

			}
		} else {
			System.out.println("BAD DATATYPE");

		}

		return -1;

	}

	public int getPageD(Table table, Object pk) throws DBAppException {
		String type = pk.getClass().getTypeName();

		if (type.equals("java.lang.Integer")) {

			int pkI = (int) pk;

			for (int i = 0; i < table.pages.size(); i++) {
				Page p = deserializePage(table.tableName, i);
				if (i == 0 && pkI < (int) p.min) {
					if (p.full == false)
						return i;
					else
						return -2;
				} else if ((pkI <= (int) p.max) && (pkI >= (int) p.min)) {

					return i;

				} else if (i == table.pages.size() - 1 && pkI > (int) p.max) {
					if (p.full == false)
						return i;
					else
						return -4;
				}

				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.lang.String")) {
			String pkS = (String) pk;
			for (int i = 0; i < table.pages.size(); i++) {
				Page p = deserializePage(table.tableName, i);

				if (i == 0 && pkS.compareTo((String) p.min) < 0) {

					if (p.full == false)
						return i;
					else
						return -2;
				} else if (pkS.compareTo((String) p.max) < 0 && pkS.compareTo((String) p.min) > 0 || pkS.equals(p.max)
						|| pkS.equals(p.min)) {

					return i;

				} else if (i == table.pages.size() - 1 && pkS.compareTo((String) p.max) > 0) {
					if (p.full == false)
						return i;
					else
						return -4;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.lang.Double")) {
			Double pkDD = (Double) pk;
			for (int i = 0; i < table.pages.size(); i++) {
				Page p = deserializePage(table.tableName, i);
				if (i == 0 && pkDD < (Double) p.min) {
					if (p.full == false)
						return i;
					else
						return -2;
				} else if (pkDD <= (Double) p.max && pkDD >= (Double) p.min) {

					return i;

				} else if (i == table.pages.size() - 1 && pkDD > (Double) p.max) {
					if (p.full == false)
						return i;
					else
						return -4;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else if (type.equals("java.util.Date")) {
			Date pkD = (Date) pk;
			for (int i = 0; i < table.pages.size(); i++) {
				Page p = deserializePage(table.tableName, i);
				if (i == 0 && pkD.compareTo((Date) p.min) < 0) {
					if (p.full == false)
						return i;
					else
						return -2;
				} else if (pkD.compareTo((Date) p.max) < 0 && pkD.compareTo((Date) p.min) > 0 || pkD.equals(p.max)
						|| pkD.equals(p.min)) {

					return i;

				} else if (i == table.pages.size() - 1 && pkD.compareTo((Date) p.max) > 0) {
					if (p.full == false)
						return i;
					else
						return -4;
				}
				p.refreshPage();
				p = null;
				System.gc();
			}
		} else {
			System.out.println("BAD DATATYPE");

		}
		return -1;

	}

	// binary search

	public void shiftPages(Table tableName, int pageNumber, Hashtable<String, Object> last) throws Exception {
		Page desiredPage;

		if (pageNumber > tableName.pages.size() - 1) {
			desiredPage = new Page(tableName.pages.size(), tableName.tableName, tableName);

			String pk = tableName.clusteringKeyName;
			Object pkValue = last.get(pk);
			desiredPage.rows.addElement(last);

			desiredPage.min = pkValue;
			desiredPage.max = pkValue;
			desiredPage.usedRows++;
			desiredPage.pk.add(pkValue);
			tableName.pages.add(desiredPage);

			desiredPage.refreshPage();
			desiredPage = null;
			System.gc();
		}

		else if (tableName.pages.get(pageNumber).full == true) {
			desiredPage = deserializePage(tableName.tableName, pageNumber);

			Hashtable<String, Object> lastInPage = desiredPage.rows.get(desiredPage.rows.size() - 1);
			String pk = tableName.clusteringKeyName;
			Object pkValue = last.get(pk);

			Object pkValue2 = lastInPage.get(pk);
			desiredPage.rows.remove(lastInPage);
			desiredPage.rows.addElement(last);

			desiredPage.pk.remove(pkValue2);
			desiredPage.pk.add(pkValue);

			desiredPage.refreshPage();
			desiredPage = null;
			System.gc();
			shiftPages(tableName, pageNumber++, lastInPage);

		} else {
			desiredPage = deserializePage(tableName.tableName, pageNumber);

			String pk = tableName.clusteringKeyName;
			Object pkValue = last.get(pk);
			desiredPage.rows.addElement(last);

			desiredPage.usedRows++;
			desiredPage.pk.add(pkValue);
			desiredPage.refreshPage();
			desiredPage = null;
			System.gc();

		}
		if (!tableName.indicies.isEmpty()) { // must check for nulls
			for (int i = 0; i < tableName.indicies.size(); i++) {
				String[] colNames = tableName.indicies.get(i)[0];
				Octree current = deserializeOctree(tableName.tableName, colNames);
				Object xOC = last.get(colNames[0]);
				Object yOC = last.get(colNames[1]);
				Object zOC = last.get(colNames[2]);

				current.updateRef(pageNumber, xOC, yOC, zOC);
				serializeOctree(tableName.tableName, colNames, current);
			}
		}
	}

	public boolean check(String strTableName, Hashtable<String, Object> htblColNameValue, File filePath)
			throws DBAppException, IOException, ParseException {
		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		boolean flag = true;
		String temp = br.readLine();
		Table t = deserializeTable(strTableName);

		while (temp != null) {

			String[] splitted = temp.split(","); // from metadata

			Set<Entry<String, Object>> valuesToBeEntered = htblColNameValue.entrySet(); // hashtable

			for (Entry<String, Object> insert : valuesToBeEntered) {

				if (strTableName.equals(splitted[0])) { // same table name

					if (insert.getKey().equals(splitted[1].trim())) { // same column name

						String enteredDataType = insert.getValue().getClass().getTypeName();

						if (enteredDataType.equals("database.Null")) {

							System.out.println(" Careful you've entered an empty value for column " + insert.getKey());

						} else {

							String columnDataType = splitted[2];

							if (!enteredDataType.equals(columnDataType)) {

								flag = false;
								throw new DBAppException(
										"Incorrect data type for entered value in column " + insert.getKey());
							}

							if (enteredDataType.equals("java.lang.Integer")) {

								int minI = Integer.parseInt(splitted[6]);
								int maxI = Integer.parseInt(splitted[7]);
								if ((int) insert.getValue() < minI || (int) insert.getValue() > maxI) {
									throw new DBAppException(insert.getKey() +" not within range");
								}
							} else if (enteredDataType.equals("java.lang.String")) {

								String minS = splitted[6];
								String maxS = splitted[7];

								if (insert.getValue().toString().toLowerCase().compareTo(minS.toLowerCase()) < 0
										|| ((String) insert.getValue()).toLowerCase()
												.compareTo(maxS.toLowerCase()) > 0) {
									throw new DBAppException(insert.getKey() +" not within range");

								}

							} else if (enteredDataType.equals("java.lang.Double")) {

								Double minDD = Double.parseDouble(splitted[6]);
								Double maxDD = Double.parseDouble(splitted[7]);
								if ((Double) insert.getValue() < minDD || (Double) insert.getValue() > maxDD) {
									throw new DBAppException(insert.getKey()+" not within range");
								}
							} else if (enteredDataType.equals("java.util.Date")) {
								String min = splitted[6];
								String max = splitted[7];
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
								Date minDate = formatter.parse(min);
								Date maxDate = formatter.parse(max);
								Date now = (Date) insert.getValue();
								if (now.before(minDate) || now.after(maxDate)) {
									throw new DBAppException(formatter.format(now) + " not within range");
								}

							}

						}

					}

				} else {
					temp = br.readLine();

				}
			}

			temp = br.readLine();

		}

		serializeTable(strTableName, t);
		t = null;
		System.gc();

		br.close();
		return flag;

	}

	// following method updates one row only
	// htblColNameValue holds the key and new value
	// htblColNameValue will not include clustering key as column name
	// strClusteringKeyValue is the value to look for to find the row to update.

	// following method could be used to delete one or more rows.
	// htblColNameValue holds the key and value. This will be used in search
	// to identify which rows/tuples to delete.
	// htblColNameValue entries are ANDED together
	public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {
		if (tableNames.isEmpty()) {
			throw new DBAppException("There are no tables to delete from, Please create a table first");// added this
																										// here
		} else {
			if (!tableNames.contains(strTableName)) {
				throw new DBAppException("cant delete from a table that is not there");
			}

			Table table = deserializeTable(strTableName);
			Set<String> x = htblColNameValue.keySet();
			for (String y : x) {
				if (!table.columnNames.contains(y)) {
					throw new DBAppException("column doesnt exist");
				}
			}
			try {
				check(strTableName, htblColNameValue, file);
			} catch (DBAppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Octree del = null;

			int indexNeeded = -25;
			if (htblColNameValue.size() >= 3) {
				// multiple indices in given hashtable ??????????
				String[][] i = checkIfIndexhtbl(htblColNameValue, table.indicies);
				String[] names = new String[3];
				names[0] = i[0][0];
				names[1] = i[0][1];
				names[2] = i[0][2];
				Object xVal = htblColNameValue.get(names[0]);
				Object yVal = htblColNameValue.get(names[1]);
				Object zVal = htblColNameValue.get(names[2]);
				Octree oc = deserializeOctree(table.tableName, names);
				ArrayList<Integer> pagenum = Octree.search(oc, xVal, "=", yVal, "=", zVal, "=");
				if (pagenum.isEmpty()) {
					System.out.println("value not found");
					return;

				}

				Set<Entry<String, Object>> valuesToBeEntered = htblColNameValue.entrySet();
				for (Integer indexneeded : pagenum) {
					Page p = deserializePage(strTableName, indexneeded);
					for (Hashtable<String, Object> row : p.rows) {
						int count = 0;
						for (Entry<String, Object> entry : valuesToBeEntered) {
							Object value = entry.getValue();
							if (row.containsValue(value)) {
								count++;
							}
						}
						if (count == htblColNameValue.size()) {
							p.pk.remove(p.rows.indexOf(row));
							p.usedRows--;
							for (String[][] indec : table.indicies) {
								String[] c = new String[3];
								c[0] = indec[0][0];
								c[1] = indec[0][1];
								c[2] = indec[0][2];
								Object ixVal = p.rows.get(p.rows.indexOf(row)).get(c[0]);
								Object iyVal = p.rows.get(p.rows.indexOf(row)).get(c[1]);
								Object izVal = p.rows.get(p.rows.indexOf(row)).get(c[2]);
								Octree ioc = deserializeOctree(strTableName, c);
								Octree.deleteNode(ioc, ixVal, iyVal, izVal);
								
								serializeOctree(strTableName, c, ioc);

							}
							if (p.usedRows == 0) {
								table.pages.remove(p);
								if (table.pages.isEmpty()) {
									table.empty = true;
								}
								p.file.delete();

							}
						}

					}
					p.refreshPage();
					p = null;
					System.gc();
				}

			}
			if (htblColNameValue.size()<3 || checkIfIndexhtbl(htblColNameValue, table.indicies) == null) {
				
				String pk = table.clusteringKeyName;
				if (htblColNameValue.containsKey(pk) && htblColNameValue.size() == 1) {
					
					Object pkValue = htblColNameValue.get(pk);
					indexNeeded = getPageD(table, htblColNameValue.get(pk));
					
					if (indexNeeded == -2 || indexNeeded == -4 || indexNeeded == -1) {

						serializeTable(strTableName, table);
						table = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					if (indexNeeded == table.pages.size() - 1) {

						Page p = deserializePage(strTableName, indexNeeded);
						Object max = p.max;
						switch (pkValue.getClass().getTypeName()) {
						case "java.lang.String":
							if (((String) pkValue).compareTo((String) max) > 0) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.lang.Integer":
							if (Integer.parseInt(pkValue.toString()) > Integer.parseInt(max.toString())) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.lang.Double":
							if (((Double) pkValue).compareTo((Double) max) > 0) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.util.Date":
							if (((Date) pkValue).after((Date) max)) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						}
						p.refreshPage();
						p = null;
						System.gc();

					}
					if (indexNeeded == 0) {

						Page p = deserializePage(strTableName, indexNeeded);
						Object min = p.min;
						switch (pkValue.getClass().getTypeName()) {
						case "java.lang.String":
							if (((String) pkValue).compareTo((String) min) < 0) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.lang.Integer":

							if (Integer.parseInt(pkValue.toString()) < Integer.parseInt(min.toString())) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.lang.Double":
							if (((Double) pkValue).compareTo((Double) min) < 0) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						case "java.util.Date":
							if (((Date) pkValue).before((Date) min)) {
								p.refreshPage();
								p = null;
								serializeTable(strTableName, table);
								table = null;
								System.gc();
								throw new DBAppException("data not available");
							}
							break;
						}
						p.refreshPage();
						p = null;
						System.gc();

					}

					Page needed = deserializePage(strTableName, indexNeeded);

					Vector<Object> temp = needed.pk;
					int index = binarySearch(needed.pk, pkValue);
					if (index == -1) {
						needed.refreshPage();
						needed = null;
						serializeTable(strTableName, table);
						table = null;
						System.gc();
						System.out.println("data not avaialble");
						return;
					}

					needed.pk.remove(index);
					needed.usedRows--;
					for (String[][] indec : table.indicies) {
						String[] c = new String[3];
						c[0] = indec[0][0];
						c[1] = indec[0][1];
						c[2] = indec[0][2];
						Object xVal = needed.rows.get(index).get(c[0]);
						Object yVal = needed.rows.get(index).get(c[1]);
						Object zVal = needed.rows.get(index).get(c[2]);
						Octree oc = deserializeOctree(strTableName, c);
						Octree.deleteNode(oc, xVal, yVal, zVal);
						
						serializeOctree(strTableName, c, oc);

					}
					if (needed.usedRows == 0) {
						table.pages.remove(needed);
						if (table.pages.isEmpty()) {
							table.empty = true;
						}
						needed.file.delete();

					} else {

						needed.refreshPage();

					}

				} else {
					
					Vector<Object> pkS = new Vector<Object>();
					Set<Entry<String, Object>> valuesToBeEntered = htblColNameValue.entrySet();

					if (indexNeeded != -25) {
						
						Page p = table.pages.get(indexNeeded);
						for (Hashtable<String, Object> row : p.rows) {
							for (Entry<String, Object> entry : valuesToBeEntered) {

								String key = entry.getKey();
								Object value = entry.getValue();
								if (row.get(key).equals(value)) {
									pkS.add(row.get(p.primaryKey));
								}
							}
						}
						p.refreshPage();

						p = null;
						System.gc();
					} else {
						
						for (int i = 0; i < table.pages.size(); i++) {
							Page p = deserializePage(strTableName, i);
							for (Hashtable<String, Object> row : p.rows) {
								
								for (Entry<String, Object> entry : valuesToBeEntered) {

									String key = entry.getKey();
									Object value = entry.getValue();
									if (row.get(key).equals(value)) {
										pkS.add(row.get(p.primaryKey));
									}
								}
							}
							
							p.refreshPage();

							p = null;
							System.gc();
						}
					}
					
					Set<Object> f = new HashSet<Object>();

					for (Object o : pkS) {
						int count = 0;
						for (Object c : pkS) {
							if (c.equals(o))
								count++;
						}
						
						if (count == htblColNameValue.size())
							f.add(o);

					}
					

					Vector<Page> tempP = new Vector<Page>();
				
					
						for (int i = 0; i < table.pages.size(); i++) {
							boolean deleted=false;
							Page p = deserializePage(strTableName, i);
						
							Vector<Object> tempR = p.pk;
						
							for(Object unique:f) {
								if(p.pk.contains(unique)) {
									int index=binarySearch(tempR, unique);
									p.pk.remove(unique);
									p.usedRows--;
								
								
								for (String[][] indec : table.indicies) {
									String[] c = new String[3];
									c[0] = indec[0][0];
									c[1] = indec[0][1];
									c[2] = indec[0][2];
									Object xVal = p.rows.get(index).get(c[0]);
									Object yVal = p.rows.get(index).get(c[1]);
									Object zVal = p.rows.get(index).get(c[2]);
									Octree oc = deserializeOctree(strTableName, c);
									Octree.deleteNode(oc, xVal, yVal, zVal);
									
									serializeOctree(strTableName, c, oc);

								}
								if (p.pk.isEmpty()) {

									
									p.file.delete();
									deleted=true;

								} else {
									p.refreshPage();
								
								}
								
								try {
									ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(p.pagePath));
									os.writeObject(p);
									os.close();
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							}
							if(!deleted) {
								tempP.add(p);
							}
							p = null;
							System.gc();
						}
					
					
					table.pages = tempP;
					
					if (table.pages.isEmpty()) 
						
						table.empty = true;
				
					
					serializeTable(strTableName, table);
					table = null;
					System.gc();

				}

			}
		}
	}

	public void updateTable(String strTableName, String strClusteringKeyValue,
			Hashtable<String, Object> htblColNameValue) throws DBAppException {
		Object pkValue = new Object();
		// if no tables are available
		if (tableNames.isEmpty()) {
			throw new DBAppException("There are no tables to insert into, Please create a table first");// added this
																										// here
		} else {
			if (!tableNames.contains(strTableName))
				throw new DBAppException("table doesnt exist");

			Table neededTable = deserializeTable(strTableName);
			Set<String> x = htblColNameValue.keySet();
			for (String y : x) {
				if (!neededTable.columnNames.contains(y)) {
					throw new DBAppException("column doesnt exist");
				}
			}
			try {
				check(strTableName, htblColNameValue, file);
			} catch (DBAppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			switch (neededTable.primaryDataType) {
			case "java.lang.String":
				pkValue = strClusteringKeyValue;

				break;
			case "java.lang.Integer":
				pkValue = Integer.parseInt(strClusteringKeyValue);
				break;
			case "java.lang.Double":
				pkValue = Double.parseDouble(strClusteringKeyValue);
				break;
			case "java.util.Date":
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH);
				try {
					pkValue = formatter.parse(strClusteringKeyValue);
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
				break;

			}
			htblColNameValue.put(neededTable.clusteringKeyName, pkValue);
			int indexNeeded = -3;

			indexNeeded = getPageD(neededTable, pkValue);
			if (indexNeeded == -2 || indexNeeded == -4 || indexNeeded == -1 || indexNeeded == -3) {
				serializeTable(strTableName, neededTable);
				throw new DBAppException("data not available");
			}
			if (indexNeeded == neededTable.pages.size() - 1) {
				Page p = deserializePage(strTableName, indexNeeded);
				Object max = p.max;
				switch (pkValue.getClass().getTypeName()) {
				case "java.lang.String":
					if (((String) pkValue).compareTo((String) max) > 0) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.lang.Integer":
					if (Integer.parseInt(pkValue.toString()) > Integer.parseInt(max.toString())) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.lang.Double":
					if (((Double) pkValue) > ((Double) max)) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.util.Date":
					if (((Date) pkValue).after((Date) max)) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				}
				p.refreshPage();
				p = null;
				System.gc();

			}
			if (indexNeeded == 0) {

				Page p = deserializePage(strTableName, indexNeeded);
				Object min = p.min;
				switch (pkValue.getClass().getTypeName()) {
				case "java.lang.String":
					if (((String) pkValue).compareTo((String) min) < 0) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.lang.Integer":

					if (Integer.parseInt(pkValue.toString()) < Integer.parseInt(min.toString())) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.lang.Double":
					if (((Double) pkValue) < ((Double) min)) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				case "java.util.Date":
					if (((Date) pkValue).before((Date) min)) {
						p.refreshPage();
						p = null;
						serializeTable(strTableName, neededTable);
						neededTable = null;
						System.gc();
						throw new DBAppException("data not available");
					}
					break;
				}
				p.refreshPage();
				p = null;
				System.gc();

			}
			Page neededPage = deserializePage(strTableName, indexNeeded);
			Vector<Object> temp = neededPage.pk;
			int index = binarySearch(neededPage.pk, pkValue);
			if (index == -1) {
				neededPage.refreshPage();
				neededPage = null;
				serializeTable(strTableName, neededTable);
				neededTable = null;
				System.gc();
				System.out.println("data not avaialble");
				return;
			}
			Hashtable<String, Object> change = neededPage.rows.get(index);
			String[][] s = checkIfIndexhtbl(htblColNameValue, neededTable.indicies);
			
			String[] names = new String[3];
			names[0] = s[0][0];
			names[1] = s[0][1];
			names[2] = s[0][2];
			Object xVal = change.get(names[0]);
			Object yVal = change.get(names[1]);
			Object zVal = change.get(names[2]);
			Octree oc = deserializeOctree(neededTable.tableName, names);
			Octree.deleteNode(oc, xVal, yVal, zVal);
			Object newValx = null;
			Object newValy = null;
			Object newValz = null;
			if (htblColNameValue.get(names[0]) != null)
				newValx = htblColNameValue.get(names[0]);
			else
				newValx = xVal;
			if (htblColNameValue.get(names[1]) != null)
				newValy = htblColNameValue.get(names[1]);
			else
				newValy = yVal;
			if (htblColNameValue.get(names[2]) != null)
				newValz = htblColNameValue.get(names[2]);
			else
				newValz = zVal;
			try {
				oc.insert(newValx, newValy, newValz, indexNeeded);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			serializeOctree(strTableName, names, oc);
			
			oc = null;
			System.gc();
			Set<Entry<String, Object>> changeSet = htblColNameValue.entrySet();
			for (Entry<String, Object> e : changeSet) {
				change.remove(e.getKey());
				change.put(e.getKey(), e.getValue());

			}

			neededPage.refreshPage();
			neededPage = null;
			serializeTable(strTableName, neededTable);
			neededTable = null;
			System.gc();

		}

	}

	public int binarySearch(Vector<Object> vect, Object x) {
		int l = 0;
		int r = vect.size() - 1;
		while (l <= r) {
			int m = l + (r - l) / 2;

			// Check if x is present at mid
			if (vect.get(m).equals(x))
				return m;

			// If x greater, ignore left half
			switch (vect.get(m).getClass().getTypeName()) {
			case "java.lang.String":
				if (((String) vect.get(m)).compareTo((String) x) < 0)
					l = m + 1;
				else
					r = m - 1;
				break;
			case "java.lang.Integer":
				if ((int) vect.get(m) < (int) x)
					l = m + 1;
				else
					r = m - 1;
				break;
			case "java.lang.Double":
				if ((Double) vect.get(m) < (Double) x)
					l = m + 1;
				else
					r = m - 1;
				break;
			case "java.util.Date":
				if (((Date) vect.get(m)).compareTo((Date) x) < 0)
					l = m + 1;
				else
					r = m - 1;
				break;
			}
			// If x is smaller, ignore right half

		}

		// if we reach here, then element was
		// not present
		return -1;
	}



	public ArrayList<String> help() {
		ArrayList<String> Op = new ArrayList<String>();
		Op.add("<");
		Op.add(">");
		Op.add(">=");
		Op.add("<=");
		Op.add("!=");
		Op.add("=");

		return Op;
	}

	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException {
		if (tableNames.isEmpty()) {
			throw new DBAppException("There are no tables to insert into, Please create a table first");

		}
		if (!tableNames.contains(arrSQLTerms[0]._strTableName)) {
			throw new DBAppException("table doesnt exist");
		}
		Table neededTable = deserializeTable(arrSQLTerms[0]._strTableName);
		if (!neededTable.columnNames.contains(arrSQLTerms[0]._strColumnName)) {
			throw new DBAppException("column doesnt exist");
		}
		ArrayList<String> Op = new ArrayList<String>();
		Op = help();
		if (!Op.contains(arrSQLTerms[0]._strOperator)) {
			throw new DBAppException("operator not supported");
		}
		Hashtable<String, Object> col = new Hashtable<String, Object>();
		col.put(arrSQLTerms[0]._strColumnName, arrSQLTerms[0]._objValue);
		try {
			Boolean b = check(neededTable.tableName, col, file);
		} catch (DBAppException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null; // when looping array change to break please!
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if (neededTable.empty == true) {

			return null;
		}

		Vector<Hashtable<String, Object>> resultSet = new Vector<Hashtable<String, Object>>();
		for (int j = 0; j < arrSQLTerms.length; j++) {

			Vector<Hashtable<String, Object>> temp = new Vector<Hashtable<String, Object>>();

			Vector<String[][]> index = neededTable.indicies;
			if (checkIfIndex(arrSQLTerms, index, strarrOperators) != null) { // check if AND
				String[][] i = checkIfIndex(arrSQLTerms, index, strarrOperators);
				String[] names = new String[3];
				names[0] = i[0][0];
				names[1] = i[0][1];
				names[2] = i[0][2];
				Object x = new Object();
				Object y = new Object();
				Object z = new Object();
				String xOp = null, yOp = null, zOp = null;
				for (SQLTerm term : arrSQLTerms) {
					if (term._strColumnName.equals(names[0])) {
						x = term._objValue;
						xOp = term._strOperator;
					}
					if (term._strColumnName.equals(names[1])) {
						y = term._objValue;
						yOp = term._strOperator;
					}
					if (term._strColumnName.equals(names[2])) {
						z = term._objValue;
						zOp = term._strOperator;
					}
				}
				Octree oc = deserializeOctree(neededTable.tableName, names);

				ArrayList<Integer> pagenum = Octree.search(oc, x, xOp, y, yOp, z, zOp);

				if (pagenum.isEmpty()) {

					return null;
				}
				for (int indexofPage : pagenum) {
					Page page = deserializePage(neededTable.tableName, indexofPage);
					Iterator<Hashtable<String, Object>> rowsIterator = page.rows.iterator();
					while (rowsIterator.hasNext()) {
						Hashtable<String, Object> inQuestion = rowsIterator.next();
						if (comparingSelect(xOp, x, names[0], inQuestion)
								&& comparingSelect(yOp, y, names[1], inQuestion)
								&& comparingSelect(zOp, z, names[2], inQuestion)) {
							if (!temp.contains(inQuestion)) {
								temp.add(inQuestion);

							}

						}

					}

					page.refreshPage();
				}
				serializeOctree(neededTable.tableName, names, oc);
				oc = null;
				System.gc();
				Vector<SQLTerm> tempterm = new Vector<SQLTerm>();
				for (SQLTerm t : arrSQLTerms) {
					if (!(t._strColumnName.equals(names[0]) || t._strColumnName.equals(names[1])
							|| t._strColumnName.equals(names[2])))
						tempterm.add(t);

				}
				SQLTerm[] f = new SQLTerm[tempterm.size()];
				for (int k = 0; k < tempterm.size(); k++) {
					f[k] = tempterm.get(k);
				}
				arrSQLTerms = f;

			} else {

				for (int i = 0; i < neededTable.pages.size(); i++) {
					Page page = deserializePage(neededTable.tableName, i);
					Iterator<Hashtable<String, Object>> rowsIterator = page.rows.iterator();
					while (rowsIterator.hasNext()) {
						Hashtable<String, Object> inQuestion = rowsIterator.next();
						if (comparingSelect(arrSQLTerms[j]._strOperator, arrSQLTerms[j]._objValue,
								arrSQLTerms[j]._strColumnName, inQuestion) == true) {
							if (!temp.contains(inQuestion))
								temp.add(inQuestion);

						}

					}

					page.refreshPage();
				}

			}
			if (j == 0) {
				resultSet = temp;
			} else {

				switch (strarrOperators[j - 1]) {

				case "AND":

					resultSet = intersection(resultSet, temp);
					break;
				case "OR":

					resultSet = union(resultSet, temp);
					break;
				case "XOR":
					resultSet = xor(resultSet, temp);
					break;
				default:
					throw new DBAppException("operator not allowed");

				}
			}
		}

		if (resultSet.isEmpty())
			return null;
		else {

			return resultSet.iterator();
		}

	}

	public String[][] checkIfIndex(SQLTerm[] arrSQLTerms, Vector<String[][]> index, String[] strarrOperators) {
		for (String[][] s : index) {
			boolean col0 = false;
			boolean col1 = false;
			boolean col2 = false;
			int x = -1;
			int y = -1;
			int z = -1;
			for (int i = 0; i < arrSQLTerms.length; i++) {
				if (s[0][0].equals(arrSQLTerms[i]._strColumnName)) {
					col0 = true;
					x = i;
				}
				if (s[0][1].equals(arrSQLTerms[i]._strColumnName)) {
					col1 = true;
					y = i;
				}
				if (s[0][2].equals(arrSQLTerms[i]._strColumnName)) {
					col2 = true;
					z = i;
				}
			}
			if (col0 && col1 && col2) {
				int a, b;
				if (x < y && x < z) {
					a = y;
					b = z;
				} else if (y < x && y < z) {
					a = x;
					b = z;
				} else {
					a = x;
					b = y;
				}
				if (strarrOperators[a - 1].equals("AND") && strarrOperators[b - 1].equals("AND"))
					return s;
			}
		}
		return null;
	}

	public String[][] checkIfIndexhtbl(Hashtable<String, Object> htbl, Vector<String[][]> index) {
		for (String[][] s : index) {
			boolean col0 = false;
			boolean col1 = false;
			boolean col2 = false;

			if (htbl.containsKey(s[0][0]))
				col0 = true;
			if (htbl.containsKey(s[0][1]))
				col1 = true;
			if (htbl.containsKey(s[0][2]))
				col2 = true;
			
			if (col0 && col1 && col2)
				return s;
		}
		return null;
	}

	public static Vector<Hashtable<String, Object>> intersection(Vector<Hashtable<String, Object>> a,
			Vector<Hashtable<String, Object>> b) {

		Vector<Hashtable<String, Object>> r = new Vector<Hashtable<String, Object>>();
		for (int i = 0; i < a.size(); i++) {
			if (a.get(i)!=null&&b.contains(a.get(i))) {
				r.add(a.get(i));
			}
		}
		return r;
	}

	public static Vector<Hashtable<String, Object>> union(Vector<Hashtable<String, Object>> a,
			Vector<Hashtable<String, Object>> b) {
		Vector<Hashtable<String, Object>> r = new Vector<Hashtable<String, Object>>();
		r = a;
		for (int i = 0; i < b.size(); i++) {
			if (b.get(i)!=null&&!r.contains(b.get(i))) {
				r.add(b.get(i));
			}
		}
		return r;
	}

	public static Vector<Hashtable<String, Object>> xor(Vector<Hashtable<String, Object>> a,
			Vector<Hashtable<String, Object>> b) {
		Vector<Hashtable<String, Object>> r = new Vector<Hashtable<String, Object>>();
		for (int i = 0; i < a.size(); i++) {
			
			if (i<b.size()&&!a.contains(b.get(i))) {
				r.add(a.get(i));
			}
		}
		for (int i = 0; i < b.size(); i++) {
			
			if (i<a.size()&&!b.contains(a.get(i))) {
				r.add(b.get(i));
			}
		}
		return r;
	}

	public static void serializeOctree(String strTableName, String[] columnNames, Octree neededOctree) {
		try {
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(
					"src/resources/" + strTableName + columnNames[0] + columnNames[1] + columnNames[2] + ".ser"));
			os.writeObject(neededOctree);
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void serializeTable(String strTableName, Table neededTable) {
		try {
			ObjectOutputStream os = new ObjectOutputStream(
					new FileOutputStream("src/resources/" + strTableName + ".ser"));
			os.writeObject(neededTable);
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean comparingSelect(String operator, Object toCompare, String colName, Hashtable<String, Object> row) {
		switch (toCompare.getClass().getTypeName()) {

		case "java.lang.String":

			String toCompareS = (String) toCompare;
			String inRowS = (String) row.get(colName);

			switch (operator) {
			case "=":

				if ((toCompareS.compareTo(inRowS)) == 0) {
					return true;
				} else {
					return false;
				}
			case ">":
				if ((inRowS.compareTo(toCompareS)) > 0) {
					return true;
				} else {
					return false;
				}
			case ">=":
				if ((inRowS.compareTo(toCompareS)) >= 0) {
					return true;
				} else {
					return false;
				}
			case "<":
				if ((inRowS.compareTo(toCompareS)) < 0) {
					return true;
				} else {
					return false;
				}
			case "<=":
				if ((inRowS.compareTo(toCompareS)) <= 0) {
					return true;
				} else {
					return false;
				}

			case "!=":

				if ((toCompareS.compareTo(inRowS)) == 0) {
					return false;
				} else {
					return true;
				}
			}

		case "java.lang.Integer":

			int toCompareI = (int) toCompare;
			int inRowsI = (int) row.get(colName);
			switch (operator) {
			case "=":

				if (toCompareI == inRowsI) {
					return true;
				} else {
					return false;
				}
			case ">":
				if (toCompareI < inRowsI) {
					return true;
				} else {
					return false;
				}
			case ">=":
				if (toCompareI <= inRowsI) {
					return true;
				} else {
					return false;
				}
			case "<":
				if (toCompareI > inRowsI) {
					return true;
				} else {
					return false;
				}
			case "<=":
				if (toCompareI >= inRowsI) {
					return true;
				} else {
					return false;
				}

			case "!=":

				if (toCompareI != inRowsI) {
					return true;
				} else {
					return false;
				}
			}

		case "java.lang.Double":

			Double toCompareD = (Double) toCompare;
			Double inRowsD = (Double) row.get(colName);
			switch (operator) {
			case "=":

				if (toCompareD.equals(inRowsD)) {

					return true;
				} else {
					return false;
				}
			case ">":
				if (toCompareD < inRowsD) {
					return true;
				} else {
					return false;
				}
			case ">=":
				if (toCompareD <= inRowsD) {
					return true;
				} else {
					return false;
				}
			case "<":
				if (toCompareD > inRowsD) {
					return true;
				} else {
					return false;
				}
			case "<=":
				if (toCompareD >= inRowsD) {
					return true;
				} else {
					return false;
				}

			case "!=":

				if (toCompareD != inRowsD) {
					return true;
				} else {
					return false;
				}
			}

		case "java.util.Date":
			Date toCompareDD = (Date) toCompare;
			Date inRowsDD = (Date) row.get(colName);
			switch (operator) {
			case "=":

				if (toCompareDD.equals(inRowsDD)) {
					return true;
				} else {
					return false;
				}
			case ">":
				if (inRowsDD.after(toCompareDD)) {
					return true;
				} else {
					return false;
				}
			case ">=":
				if (inRowsDD.after(toCompareDD) || toCompareDD.equals(inRowsDD)) {
					return true;
				} else {
					return false;
				}
			case "<":
				if (inRowsDD.before(toCompareDD)) {
					return true;
				} else {
					return false;
				}
			case "<=":
				if (inRowsDD.before(toCompareDD) || toCompareDD.equals(inRowsDD)) {
					return true;
				} else {
					return false;
				}

			case "!=":

				if (!toCompareDD.equals(inRowsDD)) {
					return true;
				} else {
					return false;
				}
			}

		}
		return false;

	}

	public static Table deserializeTable(String strTableName) {
		Table r = null;
		try {

			FileInputStream fileIn = new FileInputStream("src/resources/" + strTableName + ".ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			r = (Table) in.readObject();
			in.close();
			fileIn.close();
			return r;
		} catch (IOException i) {
			i.printStackTrace();

			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Octree deserializeOctree(String strTableName, String[] columnNames) {
		Octree r = null;
		try {

			FileInputStream fileIn = new FileInputStream(
					"src/resources/" + strTableName + columnNames[0] + columnNames[1] + columnNames[2] + ".ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			r = (Octree) in.readObject();
			in.close();
			fileIn.close();
			return r;
		} catch (IOException i) {
			i.printStackTrace();

			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Page deserializePage(String tableName, int PageNumber) {
			Page p = null;
			try {
				FileInputStream fileIn = new FileInputStream("src/resources/" + tableName + "page" + PageNumber + ".ser");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				p = (Page) in.readObject();
				in.close();
				fileIn.close();
				return p;
			} catch (IOException i) {
				i.printStackTrace();
				return p;
				// throw new DBAppException("cant find page");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return p;
	//				throw new DBAppException("cant find page");
			}
		}

	public static void main(String[] args) throws Exception {

		String strTableName = "Student";
		String strTableName2 = "lifes";

		DBApp dbApp = new DBApp();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH);
		
			
		Hashtable<String, String> htblColNameType = new Hashtable<String, String>();

		htblColNameType.put("id", "java.lang.Integer");
		htblColNameType.put("name", "java.lang.String");
		htblColNameType.put("gpa", "java.lang.Double");
		htblColNameType.put("age", "java.lang.Integer");
		htblColNameType.put("bdate", "java.util.Date");
		htblColNameType.put("job", "java.lang.String");
		htblColNameType.put("tut", "java.lang.Integer");

		Hashtable<String, String> htblColNameType2 = new Hashtable<String, String>();
		htblColNameType2.put("life", "java.lang.String");
		htblColNameType2.put("is", "java.lang.String");
		htblColNameType2.put("nice", "java.util.Date");

		Hashtable<String, String> htblColNameMin = new Hashtable<String, String>();
		htblColNameMin.put("id", "0");
		htblColNameMin.put("name", "a");
		htblColNameMin.put("gpa", "0.0");
		htblColNameMin.put("age", "15");
		htblColNameMin.put("bdate", "2000-01-01");
		htblColNameMin.put("job", "a");
		htblColNameMin.put("tut", "1");

		Hashtable<String, String> htblColNameMax = new Hashtable<String, String>();
		htblColNameMax.put("id", "1000000");
		htblColNameMax.put("name", "ZZZZZZZZZZZ");
		htblColNameMax.put("gpa", "1000000.0");
		htblColNameMax.put("age", "25");
		htblColNameMax.put("bdate", "2020-12-12");
		htblColNameMax.put("job", "mmmmmmm");
		htblColNameMax.put("tut", "40");

		Hashtable<String, String> htblColNameMax2 = new Hashtable<String, String>();
		htblColNameMax2.put("life", "ZZZZZZZZZZ");
		htblColNameMax2.put("is", "ZZZZZZZZZZ");

		htblColNameMax2.put("nice", "3000-01-01");
		Hashtable<String, String> htblColNameMin2 = new Hashtable<String, String>();
		htblColNameMin2.put("life", "a");
		htblColNameMin2.put("is", "a");

		htblColNameMin2.put("nice", "1900-01-01");
		String[] cn = { "id", "gpa", "name" };
		String[] cn2 = { "age", "job", "tut" };

		// dbApp.createTable(strTableName2, "life", htblColNameType2, htblColNameMin2,
		// htblColNameMax2);

		dbApp.createTable(strTableName, "id", htblColNameType, htblColNameMin, htblColNameMax);

		dbApp.createIndex(strTableName, cn);

		

		Hashtable htblColNameValue = new Hashtable();

		htblColNameValue.put("id", new Integer(10));
		htblColNameValue.put("name", new String("habiba"));
		htblColNameValue.put("gpa", new Double(0.9));
		htblColNameValue.put("age", new Integer(15));
		htblColNameValue.put("bdate", formatter.parse(new String("2002-01-10")));
		htblColNameValue.put("job", new String("lord"));
		htblColNameValue.put("tut", new Integer(9));
//
		dbApp.insertIntoTable(strTableName, htblColNameValue);
//
		htblColNameValue = new Hashtable();
		htblColNameValue.put("id", new Integer(1));
		htblColNameValue.put("name", new String("sara"));
		htblColNameValue.put("gpa", new Double(0.55));
		htblColNameValue.put("age", new Integer(21));
		htblColNameValue.put("bdate", formatter.parse(new String("2003-01-10")));
		htblColNameValue.put("job", new String("barber"));
		htblColNameValue.put("tut", new Integer(24));

		dbApp.insertIntoTable(strTableName, htblColNameValue);

		htblColNameValue = new Hashtable();
		htblColNameValue.put("id", new Integer(600000));
		htblColNameValue.put("name", new String("mariam"));
		htblColNameValue.put("gpa", new Double(0.5));
		htblColNameValue.put("age", new Integer(20));
		htblColNameValue.put("bdate", formatter.parse(new String("2002-11-20")));
		htblColNameValue.put("job", new String("lehrerin"));
		htblColNameValue.put("tut", new Integer(15));

		dbApp.insertIntoTable(strTableName, htblColNameValue);
		dbApp.createIndex(strTableName, cn2);
		htblColNameValue.put("id", new Integer(60));
		htblColNameValue.put("name", new String("mariam"));
		htblColNameValue.put("gpa", new Double(0.5));
		htblColNameValue.put("age", new Integer(20));
		htblColNameValue.put("bdate", formatter.parse(new String("2002-11-20")));
		htblColNameValue.put("job", new String("lehrerin"));
		htblColNameValue.put("tut", new Integer(15));

		dbApp.insertIntoTable(strTableName, htblColNameValue);
		htblColNameValue.put("id", new Integer(700000));
		htblColNameValue.put("name", new String("mariam"));
		htblColNameValue.put("gpa", new Double(0.5));
		htblColNameValue.put("age", new Integer(20));
		htblColNameValue.put("bdate", formatter.parse(new String("2002-11-20")));
		htblColNameValue.put("job", new String("lehrerin"));
		htblColNameValue.put("tut", new Integer(15));

		dbApp.insertIntoTable(strTableName, htblColNameValue);


		
//		Table table = deserializeTable(strTableName);
//
//	
//
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("id", new Integer(8));
//		htblColNameValue.put("name", new String("kareem"));
//		htblColNameValue.put("gpa", new Double(90.3));
//
//		dbApp.insertIntoTable(strTableName, htblColNameValue);
		 //Table table = deserializeTable(strTableName);
//		for (int i = 0; i < table.pages.size(); i++) {
//			Page p = deserializePage("Student", i);
//
//			for ( int j=0; j< p.rows.size();j++) {
//				System.out.println(p.rows.get(j));
//			}
//			System.out.println();
//		}
		
		htblColNameValue = new Hashtable();
		
		htblColNameValue.put("name", new String("mariam"));
		//htblColNameValue.put("gpa", new Double(0.9));
//
//		dbApp.updateTable(strTableName, "8", htblColNameValue);
		dbApp.deleteFromTable(strTableName, htblColNameValue);
//		
//		
//		
//		Table t=deserializeTable(strTableName);
//		System.out.println(t.pages.get(0).rows);
		
//
//		
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("id", new Integer(5));
//		htblColNameValue.put("name", new String("karen"));
//		htblColNameValue.put("gpa", new Double(90.1));
//
//		dbApp.insertIntoTable(strTableName, htblColNameValue);
//		
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("id", new Integer(100));
//		htblColNameValue.put("name", new String("malak"));
//		htblColNameValue.put("gpa", new Double(0.93));
//
//		dbApp.insertIntoTable(strTableName, htblColNameValue);
//		deserializeTable(strTableName);
//		System.out.println(table.pages.size());
//		for (int i = 0; i < table.pages.size(); i++) {
//			Page p = deserializePage("Student", i);
//
//			for ( int j=0; j< p.rows.size();j++) {
//				System.out.println(p.rows.get(j));
//			}
//			System.out.println();
//		}
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("id", new Integer(7));
//		htblColNameValue.put("name", new String("laila"));
//		htblColNameValue.put("gpa", new Double(9.9));
//
//		dbApp.insertIntoTable(strTableName, htblColNameValue);
//		for (int i = 0; i < table.pages.size(); i++) {
//			
//			Page p = deserializePage("Student", i);
//
//			for ( int j=0; j< p.rows.size();j++) {
//				System.out.println(p.rows.get(j));
//			}
//			System.out.println("\n");
//			
//		}
//		serializeTable(strTableName, table);
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("life", new String("sara"));
//		htblColNameValue.put("is", new String("kareem"));
//		Date inserting = formatter.parse("2005-05-19");
//		htblColNameValue.put("nice", inserting);
//
//		dbApp.insertIntoTable(strTableName2, htblColNameValue);
//		
//		Hashtable<String, Object> htblColNameValue2 = new Hashtable<String, Object>();
//		htblColNameValue2.put("life", new String("baby"));
//		htblColNameValue2.put("is", new String("x"));
//		inserting = formatter.parse("1980-01-01");
//		htblColNameValue2.put("nice", inserting);
//
//		dbApp.insertIntoTable(strTableName2, htblColNameValue2);
//
//		htblColNameValue = new Hashtable();
//		htblColNameValue.put("life", new String("x"));
//		htblColNameValue.put("is", new String("sa"));
//		inserting = formatter.parse("2001-12-1");
//		htblColNameValue.put("nice", inserting);
//
//		dbApp.insertIntoTable(strTableName2, htblColNameValue);
//		
		// dbApp.createTable(strTableName, "life", htblColNameType2, htblColNameMin2, htblColNameMax2);
		
		//plan2=deserializeOctree(strTableName, cn);
	
//		plan2.dfs(plan2);
//		for (int i = 0; i < table.pages.size(); i++) {
//			Page p = deserializePage("Student", i);
//
//			for ( int j=0; j< p.rows.size();j++) {
//				System.out.println(p.rows.get(j));
//			}
//			System.out.println();
//		}
		


	}

}
