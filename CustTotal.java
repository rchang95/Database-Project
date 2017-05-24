import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.awt.FlowLayout;
import java.io.*;
import java.sql.*;
import javax.swing.*;

public class CustTotal {
	private Connection conDB;   // Connection to the database system.
	private String url;         // URL: Which database?

	private Integer custID;     // Who are we tallying?
	private String  custName;   // Name of that customer.
	private String cityName;

	private ArrayList<String> categoryList;
	private ArrayList<String> bookList;
	private double amount;
	private String category;	
	private double totalAmt;
	private String clubName;
	private String bookName;
	private String bookyear;

	public CustTotal (int a){
		try {
			Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
		} catch (InstantiationException e) {				
			e.printStackTrace();
			System.exit(0);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}

		url = "jdbc:db2:C3421M";

		try {
			conDB = DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.out.println("\nSQL: database connection error.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		try{
			custID = new Integer(a);
		} catch (NumberFormatException e) {
			System.out.println("\nUsage: java CustTotal cust#");
			System.out.println("Provide an INT for the cust#.");
			System.exit(0);
		}


		while (!customerCheck()) {
			String s = "There is no customer # " + custID + " in the database"; 
			JFrame f1 = new JFrame("Message");
			JOptionPane.showMessageDialog(f1, s);

			String newNum = JOptionPane.showInputDialog(f1, "Please re-enter the customer id");
			int newNum1 = Integer.parseInt(newNum);
			custID = newNum1;

		}
		//	System.out.println();

		JFrame f2 = new JFrame("Customer");
		String result = find_customer(custID.intValue());
		JOptionPane.showMessageDialog(f2, result);
		//	System.out.println("\n");

		
			categoryList = fetch_categories();
			System.out.println("========List of Categories:==========");
			int j=1;
			for (int i=0; i<categoryList.size(); i++) {
				System.out.println("( "+ j + " ) " + categoryList.get(i));
				j++;
				} 
		do {
			Scanner op = new Scanner(System.in);
			System.out.print("\nPlease choose a category from the list: ");
			int input = op.nextInt();
			while (input < 1 || input > categoryList.size()) {
				System.out.println("Invalid Choice!!");
				System.out.println("Please re-enter the choice for a category from the list: ");
				input = op.nextInt();
			}
			category = categoryList.get(input-1);
			String msg4 = "the catgegory chosen is: " + category;
			JOptionPane.showMessageDialog(f2, msg4);
				
			String bookTitle = JOptionPane.showInputDialog(f2, "Please enter a book title");
			bookList = find_book(category, bookTitle);
		
		} while(bookList.isEmpty());
		
		int count = 0;
		String t = "";
		String[] variable = {"Title: ", "Year: ", "Language: ", "Weight: "};
		for(int i=0; i<bookList.size(); i++) {
		   if(count < variable.length) {
			t = t + variable[count] + bookList.get(i) + "\n";
			count++;
		    } else {
			count = 0;
			t = t + "\n" + variable[count] + bookList.get(i) + "\n";
			count++;
			}
		}
		JOptionPane.showMessageDialog(f2, t);
		bookName = bookList.get(0);
		bookyear = bookList.get(1); 
		clubName = getClubName(custID.intValue(), bookName, bookyear);		
//		JOptionPane.showMessageDialog(f2, c);

		double amt = min_price(category, bookName, bookyear, custID.intValue());	
		String msg = "The price for " + bookName + " is $" + amt + "\nHow many would you like to buy?";
		String qnty = JOptionPane.showInputDialog(f2, msg);
		int qnty1 = Integer.parseInt(qnty);
		totalAmt = (qnty1 * amt);
		
		totalAmt = Math.round(totalAmt * 100);
		totalAmt = totalAmt/100;

		String msg1 = "Your bill is $" + totalAmt;
		JOptionPane.showMessageDialog(f2, msg1);

		String msg2 = "Confirm purchase of \n Book: " + bookName + " for $" + totalAmt;
        
        	if(JOptionPane.showOptionDialog(f2, msg2, "Confirm Order", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE , null, null, null) == JOptionPane.YES_OPTION ){
			insert_purchase(custID.intValue(), clubName, bookName, bookyear, qnty1);
        		JOptionPane.showMessageDialog(f2, "Changes successfully made! Database has been updated!");
        	} 
		
		
			try {
				conDB.close();
				System.exit(0);
			} catch(SQLException e) {
				System.out.print("\nFailed trying to close the connection.\n");
				e.printStackTrace();
				System.exit(0);
			
		}    
	}
	public boolean customerCheck() {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.

		boolean           inDB      = false;  // Return.

		queryText =
				"SELECT name       "
						+ "FROM yrb_customer "
						+ "WHERE cid = ?     ";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			querySt.setInt(1, custID.intValue());
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				inDB = true;

			} else {
				inDB = false;
				custName = null;
				cityName = null;
			}
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return inDB;
	}
	public String find_customer(int id) {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.
		String info = "";
		queryText =
				"SELECT *       "
						+ "FROM yrb_customer "
						+ "WHERE cid = ?     ";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			querySt.setInt(1, id);
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				custName = answers.getString("name");
				cityName = answers.getString("city");
				info = "Customer id: "+ custID.toString() + "\nName: "+ custName + "\nCity: " + cityName;
			} else {
				custName = null;
				cityName = null;
			}
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return info;
	}

	public ArrayList<String> fetch_categories() {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.
		ArrayList<String> category = new ArrayList<String>();

		queryText =
				"SELECT *       "
						+ "FROM yrb_category ";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try { 
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {

			while (answers.next()) {
				category.add(answers.getString("cat"));
			} 

		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}
		return category;
	}
	
	public ArrayList<String> find_book(String cat, String book) {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.
		ArrayList<String> bookInfo = new ArrayList<String>();

		queryText =
		  "SELECT *       "
		+ "FROM yrb_book B "
		+ "WHERE B.cat = ? AND B.title = ?";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {
			querySt.setString(1, cat);
			querySt.setString(2, book);
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {

			while (answers.next()) {
				bookInfo.add(answers.getString("title"));
				bookInfo.add(answers.getString("year"));
				bookInfo.add(answers.getString("language"));
				bookInfo.add(answers.getString("weight"));
			} 

		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}
		return bookInfo;
	}

	public double min_price(String cat, String bookTitle, String yr, int id) {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.

		boolean           inDB      = false;  // Return.

		queryText =
			  "SELECT MIN(F.price) as price      "
			+ "FROM yrb_offer F, yrb_member M "
			+ "WHERE M.cid = ? AND M.club = F.club AND F.title = ? AND F.year = ?   ";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			querySt.setInt(1, custID.intValue());
			querySt.setString(2, bookTitle);
			querySt.setString(3, yr);
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				amount = answers.getDouble("price");

			} 
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return amount;
	}
	
	public void insert_purchase(int id, String club, String bookTitle, String yr, int quantity) {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
	//	ResultSet         answers   = null;   // A cursor.
		int ans = 0;
		boolean           inDB      = false;  // Return.
		
		int y = Integer.parseInt(yr);
		queryText =
		//	  "INSERT INTO yrb_purchase(cid, club, title, year, qnty) VALUES (?, ?, ?, ?, ?) ";
			"INSERT INTO yrb_purchase VALUES (?, ?, ?, ?, '2017-04-03-8.45.00', ?)";
		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {
			
			querySt.setInt(1, id);
			querySt.setString(2, club);
			querySt.setString(3, bookTitle);
			querySt.setInt(4, y);
			querySt.setInt(5, quantity);
			ans = querySt.executeUpdate();
			
			if(ans==0) {
			   System.out.println("Insertion failed!");	
			} else {
			   System.out.println("Successful!");
			}
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
	/*	try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}
*/
		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

	}

	public String getClubName(int id, String title, String yr) {
		String            queryText = "";     // The SQL text.
		PreparedStatement querySt   = null;   // The query handle.
		ResultSet         answers   = null;   // A cursor.
		String info = "";
		queryText =
				"SELECT M.club     "
				+ "FROM yrb_member M, yrb_offer F "
				+ "WHERE M.cid = ? AND M.club = F.club AND F.title = ? AND F.year = ?  ";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			querySt.setInt(1, id);
			querySt.setString(2, title);
			querySt.setString(3, yr);
			answers = querySt.executeQuery();
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				clubName = answers.getString("club");
			}
		} catch(SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch(SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return clubName;
	}

	public static void main(String[] args) {
		//	Scanner sc = new Scanner(System.in);
		//	System.out.print("Please enter a customer ID: ");
		//	int num = sc.nextInt();

		JFrame f = new JFrame("Dialog Box");
		String n = JOptionPane.showInputDialog(f, "Please enter a customer id ");
		int num = Integer.parseInt(n);	
		CustTotal ct = new CustTotal(num);   
		
	

		
	}

}
