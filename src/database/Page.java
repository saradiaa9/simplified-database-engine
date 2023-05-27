package database;

import java.io.*;
import java.util.*;
import java.util.function.BinaryOperator;

public class Page implements Serializable {
	public Object min, max;
	public Vector<Object> pk = new Vector<Object>();
	public String primaryKey;
	public Vector<Hashtable<String, Object>> rows = new Vector<Hashtable<String, Object>>();
	public int maxrows; // must change to get from the resources file
	public int usedRows = 0;
	public boolean full = false;
	public String pagePath, newpagePath;
	public Table t;
	public File file, newfile;
	public String primaryKeyType;

	public Page(int number, String tableName, Table t) throws Exception {
		this.t = t;
		String configFilePath = "src/resources/DBApp.config";
		FileInputStream propsInput = new FileInputStream(configFilePath);
		Properties prop = new Properties();
		prop.load(propsInput);
		maxrows = Integer.parseInt(prop.getProperty("MaximumRowsCountinTablePage"));
		pagePath = "src/resources/" + tableName + "page" + number + ".ser";
		file = new File(pagePath);
		file.createNewFile();
//		newpagePath = "src/resources/ex"+tableName+".ser";
//		newfile=new File(pagePath);
//		newfile.createNewFile();
		primaryKey = t.clusteringKeyName;
		primaryKeyType = t.primaryDataType;

	}

	public void refreshPage() { // sorts and updates min and max

		this.binarySort();
	
		Vector<Hashtable<String, Object>> newrows = new Vector<Hashtable<String, Object>>();

		for (Object p : pk) {

			for (Hashtable<String, Object> t : rows) {

				if (t.get(primaryKey).equals(p)) {

					newrows.add(t);
					break;
				}
			}

		}
		min = pk.get(0);
		max = pk.get(pk.size() - 1);
		rows = newrows;

		try {
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(pagePath));
			os.writeObject(this);
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (usedRows == maxrows) {
			full = true;
		}

	}


	

	public void binarySort() {
		switch (primaryKeyType) {
		case "java.lang.Integer":
			for (int i = 1; i < this.pk.size(); i++) {
				int key = (int) this.pk.get(i);
				Hashtable<String, Object> correctRow = this.rows.get(i);
				int position = binarySearch2( i, key);
				int j = i;
				while (j > position) {

					this.pk.set(j, this.pk.get(j - 1));
					this.rows.set(j, this.rows.get(j - 1));
					j = j - 1;
				}
				this.pk.set(position, key);
				this.rows.set(position, correctRow);
				//System.out.println(rows);
			}
			break;
		case "java.lang.String":
			for (int i = 1; i < this.pk.size(); i++) {
				String key = (String) this.pk.get(i);
				Hashtable<String, Object> correctRow = this.rows.get(i);
				int position = binarySearch2( i, key);
				int j = i;
				while (j > position) {

					this.pk.set(j, this.pk.get(j - 1));
					this.rows.set(j, this.rows.get(j - 1));
					j = j - 1;
				}
				this.pk.set(position, key);
				this.rows.set(position, correctRow);
				//System.out.println(rows);
			}
			break;
		case "java.lang.Double":
			for (int i = 1; i < this.pk.size(); i++) {
				Double key = (Double) this.pk.get(i);
				Hashtable<String, Object> correctRow = this.rows.get(i);
				int position = binarySearch2( i, key);
				int j = i;
				while (j > position) {

					this.pk.set(j, this.pk.get(j - 1));
					this.rows.set(j, this.rows.get(j - 1));
					j = j - 1;
				}
				this.pk.set(position, key);
				this.rows.set(position, correctRow);
				//System.out.println(rows);
			}
			break;
		case "java.util.Date":
			for (int i = 1; i < this.pk.size(); i++) {
				Date key = (Date) this.pk.get(i);
				Hashtable<String, Object> correctRow = this.rows.get(i);
				int position = binarySearch2( i, key);
				int j = i;
				while (j > position) {

					this.pk.set(j, this.pk.get(j - 1));
					this.rows.set(j, this.rows.get(j - 1));
					j = j - 1;
				}
				this.pk.set(position, key);
				this.rows.set(position, correctRow);
				//System.out.println(rows);
			}
			break;
		}
	}

	public int binarySearch2(int whereToStop, Object keytoSearchfor) {
		int l = 0;
		int r = whereToStop;
		switch (primaryKeyType) {
		case "java.lang.Integer":
			int pkValueI = (int) keytoSearchfor;

			while (l < r) {
				int mid = (l + r) / 2;
				if ((int) this.pk.get(mid) <= pkValueI) {
					l = mid + 1;
				} else {
					r = mid;
				}
			}
			return l;
			
		case "java.lang.String":
			String pkValueS = (String) keytoSearchfor;

			while (l < r) {
				int mid = (l + r) / 2;
				if (((String) this.pk.get(mid)).compareTo(pkValueS) == 0
						|| ((String) this.pk.get(mid)).compareTo(pkValueS) == -1) {
					l = mid + 1;
				} else {
					r = mid;
				}
			}
			return l;
			
		case "java.lang.Double":
			Double pkValueD = (Double) keytoSearchfor;

			while (l < r) {
				int mid = (l + r) / 2;
				if ((Double) this.pk.get(mid) <= pkValueD) {
					l = mid + 1;
				} else {
					r = mid;
				}
			}
			return l;
			
		case "java.util.Date":
			Date pkValueDD = (Date) keytoSearchfor;

			while (l < r) {
				int mid = (l + r) / 2;
				if (((Date) this.pk.get(mid)).before(pkValueDD)||((Date) this.pk.get(mid)).equals(pkValueDD)) {
					l = mid + 1;
				} else {
					r = mid;
				}
			}
			return l;
			
		}
		return l;

	}

public static void main(String[] args) {

	}}