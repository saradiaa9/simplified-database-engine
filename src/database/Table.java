package database;

import java.io.*;
import java.util.*;




public class Table implements Serializable {
	public String tableName;
	public boolean empty;
	public Vector<Page> pages= new Vector<Page>();
	File file;
	public String clusteringKeyName;
	public int columnNum;
	public Vector<String> columnNames=new Vector<String>();
	public String primaryDataType;
	public Vector<String [][]> indicies=new Vector<String[][]>();//[column name,min,]
	
	public Table() {
		
	}
	
	
	public Table(String strTableName, String strClusteringKeyColumn, Hashtable<String, String> htblColNameType,
			Hashtable<String, String> htblColNameMin, Hashtable<String, String> htblColNameMax, File filepath) throws IOException {

		this.tableName = strTableName;
		empty=true;
		clusteringKeyName=strClusteringKeyColumn;
		primaryDataType=htblColNameType.get(strClusteringKeyColumn);
		
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(filepath,true));
		

		Set<String> keys = htblColNameType.keySet();
		for (String key : keys) {
			boolean primary = false;
			if (key == strClusteringKeyColumn) {
				
				primary = true;
				
			}
			try {

				bWriter.append(strTableName + "," + key + "," + htblColNameType.get(key) + "," + primary + "," + null
						+ "," + null + "," + htblColNameMin.get(key) + "," + htblColNameMax.get(key));
				bWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		bWriter.close();
		String pagePath = "src/resources/"+tableName+".ser";
		file=new File(pagePath);
		file.createNewFile();
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
		
	}
	public void refreshTable () {
		
	}

	
	
	
}
