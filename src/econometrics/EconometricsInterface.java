package econometrics;

import ecoDB.Country;
import ecoDB.CountryData;
import ecoDB.CountryDataJpaController;
import ecoDB.CountryDataset;
import ecoDB.CountryDatasetJpaController;
import ecoDB.CountryJpaController;
import java.io.FileReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import javax.swing.table.DefaultTableModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.Objects;
import org.jfree.ui.RefineryUtilities;


/**
 *
 * @author moonlapsevertigo
 */

public final class EconometricsInterface extends javax.swing.JFrame {

    public final String key = "1yq6zx7pcAS-x26YRA5M"; //api key
    // για windows θέλει ".\\resources\\iso-countries.csv"
    // για mac/linux βάζουμε "./resources/iso-countries.csv"
    public final String csvFile = "./resources/iso-countries.csv";
    // Βάζουμε σε μία μεταβλητή την τελευταία μέρα του χρόνου (LDY, Last Day of the Year)
    // Αυτό είναι για να εμφανίζει στο startDate την τελευταία μέρα.
    public final String ldy = "31 Dec ";

    // Το όνομα EconometricsPU το παίρνουμε από το αρχείο peristence.xml
    // Entity Manager
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("EconometricsPU");
    EntityManager em = emf.createEntityManager();

    // Controllers για να "μιλάμε" με τα tables της βάσης.
    // Το Netbeans φτιάχνει αυτόματα τις κλάσεις για τους controllers
    CountryJpaController cjc = new CountryJpaController(emf);
    CountryDatasetJpaController cdsc = new CountryDatasetJpaController(emf);
    CountryDataJpaController cdc = new CountryDataJpaController(emf);

    /**
     * Creates new form EconometricsInterface
     */
    public EconometricsInterface() throws Exception {
        initComponents();
        // Μέθοδος για δημιουργία του μενού
        csvDataToDropDownMenu();
    }// end void

    // Γεμίζουμε την βάση
    public void fillTableCountry(List<Country> countries) throws Exception {
        // Ελέγχουμε αν η βάση και συγκεκριμένα το table Country είναι άδειο.
        // Αν είναι άδειο, περνάμε τα στοιχεία των χωρών στο table Country της βάσης μας. 
        for (Country c : countries) {
            Country newCountry = new Country();
            newCountry.setIsoCode(c.getIsoCode());
            newCountry.setName(c.getName());
            // Με τον controller του table Country, ανεβάζουμε τα δεδομένα στο table
            // Η create είναι μέθοδος του CountryJpaController την οποία
            // δημιουργεί αυτόματα το netbeans και παίρνει ως παράμετρο ένα αντικείμενο τύπου Country.
            cjc.create(newCountry);
        } // end for
    }// end void

   //Η μέθοδος διαβάζει το αρχείο και εισάγει το όνομα της χώρας
    //στο drop-down menu.
    public void csvDataToDropDownMenu() throws Exception {
        // Όταν Αρχίζει το app και δεν υπάρχει επιλογή, το save και το plot είναι απενεργοποιημένα. 
        // Το CheckBox είναι πάντα απενεργοποιημένο γιατί δεν θέλουμε ο χρήστης να το "ελέγχει"
       
        CheckBox.setEnabled(false);
        CheckBox.setSelected(false);
        
        // Αρχικά To save είναι απενεργοποιημένο.
        save.setEnabled(false);
       
        
        // Φτιάχνουμε μία λίστα με αντικείμενα Country. 
        // Καλούμε την μέθοδο countryDataFromCSV η οποία επιστρέφει λίστα με όλες τις χώρες που διάβασε από το csv
        List<Country> countries = countryDataFromCSV(csvFile);

        if (cjc.findCountryEntities().isEmpty()) {
            fillTableCountry(countries);
        }// end if

        // Φτιάχνουμε ένα ComboBoxModel στο οποίο θα βάλουμε τα ονόματα των χωρών
        // Το κάνουμε σε ξεχωριστό loop γιατί το μενού θέλουμε να παμένει ακόμη και αν διαγραφούν τα δεδομένα.
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
        countries.forEach((c) -> {
            comboModel.addElement(c.getName());
        }); // end for

        // Θέτουμε σε λειτουργία το μενού.
        countrySelection.setModel(comboModel);
        // Βάζουμε τη σημαία της 1ης χώρας σαν αρχική
            flags.setIcon(new javax.swing.ImageIcon(getClass().getResource("/flags/AFG.png"))); 
    }// end void

// Η μέθοδος countryDataFromCSV, επιστρέφει μία λίστα με ονόματα χωρών που διάβασε από το csv.   
    private static List<Country> countryDataFromCSV(String csvFile) {

        List<Country> countries = new ArrayList<>();

        //Η μεταβλητή cvsSplitBy θα χρησιμοποιηθεί για τον προσδιορισμό
        //του σημείου στίξης (;) που διαχωρίζει τα δεδομένα στο 
        //αρχείο iso-countries.csv
        String line;
        String cvsSplitBy = ";";
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                //Δημιουργία πίνακα όπου αποθηκεύονται όλα τα δεδομένα
                //του αρχείου iso-countries.csv ανά στήλη και γραμμή 
                //χρησιμοποιοώντας το σημείο στήξης (;)
                String[] c = line.split(cvsSplitBy);
                // Mε την createCountry γφτιάχνουμε αντικείμενα
                // Country κρατώντας μόνο το όνομα και τον κωδικό της κάθε χώρας.
                Country country = createCountry(c);
                countries.add(country);
            }    // end while
        } catch (IOException e) {
             JOptionPane.showMessageDialog(null, "IOEcxeption has been caught ", "INFO", JOptionPane.INFORMATION_MESSAGE);
        }
        return countries;
    }// end void

// Η μέθοδος createCountry δημιουργεί αντικείμενα Country
    private static Country createCountry(String[] countryData) {
        //Εισάγουμε σε 2 String το isoCode και το name της Χώρας (3η και 1η στήλη του csv)
        String isoCode = countryData[2];
        String name = countryData[0];
        // Δημιουργούμε  αντικείμενο Country και το επιστρέφουμε 
        return new Country(isoCode, name);
    }// end void
    
// Επιστρέφει ένα JsonObject από μία url     
    public static JsonObject stringToJasonObject(URL url) throws MalformedURLException {
        
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        JsonObject jsonObj = null;
         try (Response response = client.newCall(request).execute()){
             if (response.isSuccessful() && response.body() != null) {
                 String responseString = response.body().string();
                 Gson gson = new Gson();
                 JsonElement element = gson.fromJson(responseString, JsonElement.class);
                 jsonObj = element.getAsJsonObject(); 
             }
         }catch (IOException e) {
               JOptionPane.showMessageDialog(null, "Check your internet connection :) ", "error", JOptionPane.ERROR);
            return null;
        }
        return jsonObj;
    }

    public void JsonToJava(String isoCodeFind) throws IOException, Exception {

        // Αυτό μπορεί και να είναι περιττό, αλλά προγραμματιστικά μπορεί να είναι σωστό 
        String countryCode = isoCodeFind;

        // Φτιάχνουμε το λινκ. Ένα για το OIL και ένα για το GDP
        URL urlGDP = new URL("https://www.quandl.com/api/v3/datasets/WWDI/" + countryCode + "_NY_GDP_MKTP_CN.json?api_key=" + key);
        URL urlOIL = new URL("https://www.quandl.com/api/v3/datasets/BP/OIL_CONSUM_" + countryCode + ".json?api_key=" + key);

       
        // Φτιάχνουμε ένα αντικείμενο JsonObject για να βάζουμε τα αποτελέσματα
        // Τα δεδομένα θα έρθουν σε μορφή string. Θα τα μετατρέψουμε σε JsonObject
        // για να μπορούμε να τα διαχειριστούμε
        JsonObject objResultOIL = null;
        JsonObject objResultGDP = null;

        // Πρώτα θα σχοληθούμε με τα δεδομένα για το OIL
        try {
            
            // Στο objResultOIL το οποίο είναι JsonObject θα βάλουμε τα δεδομένα που θα πάρουμε από το 
            // request που στέλνουμε με την χρήση URL           
            objResultOIL = stringToJasonObject(urlOIL);
 
            // Για να πάρουμε τα αποθηκευμένα δεδομένα
            // Επιλέγουμε "dataset", γιατί αν δείτε μέσα εκεί υπάρχουν οι πληροφορίες για την χώρα
            // βάζουμε το όνομα της χώρα το οποίο το παίρνουμε από το objResultOIL και "πετάμε" του 18 πρώτους χαρακτήρες
            // Με πράσινο χρώμα είναι το όνομα του κάθε πλαισίου, το οποίο αντιστοιχεί σε ένα χώρο στο app
            // Δηλαδή το countryNameForOilData, αν δείτε στην καρτέλα Design είναι ο χώρος όπου πρέπει να βάλουμε το όνομα της χώρας.
            countryNameForOilData.setText(objResultOIL.getAsJsonObject("dataset").get("name").toString().substring(19,objResultOIL.getAsJsonObject("dataset").get("name").toString().length()-1 ).toUpperCase());
            startDateOil.setText(ldy + objResultOIL.getAsJsonObject("dataset").get("start_date").toString().substring(1, 5));
            endDateOil.setText(ldy + objResultOIL.getAsJsonObject("dataset").get("end_date").toString().substring(1, 5));

            // Οι πληροφορίες για την κατανάλωση φυλάσονται στον φάκελο με το όνομα  "dataset"
            // και μέσα σε αυτό υπάρχει ο φάκελο "data" ο οποίος περιέχει μία λίστα με δύο στήλες
            // στην 1η υπάρχει το έτος με τον μήνα και την ημέρα (θα κρατήσουμε μόνο το έτος)
            // στη 2η στήλη υπάρχει ο αριθμός κατανάλωσης για το συγκεκριμένο έτος
            // οπότε για να τα πάρουμε θέλουμε ένα for
            // αφού φτιάξουμε πρώτα ένα αντικείμενο JsonArray και βάλουμε μέσα εκεί ότι έχει ο φάκελο "data"
            // Αυτά τα βρήκα στο http://jsonviewer.stack.hu/
            // αν ρίξουμε μέσα τα αποτελέσματα που θα επιστρέφει το λινκ
            // εδώ φτιάχνουμε πίνακα, γιατί τα δεδομένα για την κατανάλωση είναι σε πίνακα
            JsonArray jaOIL = objResultOIL.getAsJsonObject("dataset").get("data").getAsJsonArray();

            // TableModel είναι ο χώρος όπου θα μπουν τα αποτελέσματα της κατανάλωσης
            // Πρέπει πρώτα να φτιάξουμε ένα αντικείμενο της κλάσης DefaultTableModel
            DefaultTableModel tableDataModelOIL = new DefaultTableModel();

            // Θέλουμε να εμφανίζει όνομα σε κάθε στήλη. Αν δεν το κάνουμε δεν θα έχουμε επικεφαλίδα
            tableDataModelOIL.setColumnIdentifiers(new String[]{"Year", "Value"});

            // Το jaOIL είναι ο πίνακας που έχει τα δεδομένα που θέλουμε να εμφανιστούν
            // Διατρέχουμε τον πίνακα και σε κάθε γραμμή θα βάζουμε το έτος και δίπλα την κατανάλωση
            for (int i = 0; i < jaOIL.size(); i++) {
                // Στην year βάζουμε το έτος, μόνο το έτος γι'αυτό κόβουμε ότι περισσεύει
                // Βάζουμε σε μία μεταβλητή τύπου String το περιεχόμενο του πίνακα της θέσης i
                // Το συγκερκιμένο string είναι κάπως έτσι "["2018-12-31",15.735100014]"
                String str = jaOIL.get(i).toString();
                // Για το πάρουμε το έτος πετάμε τους δύο πρώτους χαρακτήρες και κρατάμε τους υπόλοιπους 4
                String year = str.substring(2, 6);
                // Για να πάρουμε την κατανάλωση αρχίζουμε από τον 14ο χαρακτήρα μέχρι τον προτελευταίο
                String dataOIL = str.substring(14, str.length() - 1);
                // Συνθέτουμε τα δεδομένα στο modeλ. Σε κάθε γραμμή βάζουμε έτος και κατανάλωση
                tableDataModelOIL.addRow(new String[]{year, dataOIL});
            } // end for

            // Με πράσινο χρώμα το όνομα του χώρου που θέλουμε να εφμανίζεται ο πίνακας με τα δεδομένα
            tableDataOIL.setModel(tableDataModelOIL);

        } catch (Exception e) {
            // Εδώ μπαίνει αν δεν έχει dataOIL η χώρα
            // Φτιάχνουμε ξανά DefaultTableModel, γιατί το άλλο είναι στο try
            // Αν δεν το κάνουμε, θα κρατήσει τα δεδομένα της προηγούμενης αναζήτησης.
            // Ουσιαστικά αδειάζουμε τα πάντα, αλλά μόνο από το ΟIL.
            cleanDataFromInterface("oil");
            JOptionPane.showMessageDialog(null, "There is no OIL data for this country ", "INFO", JOptionPane.INFORMATION_MESSAGE);
        }
        // Κάνουμε ακριβώς το ίδιο για το GDP, όπου Oil το έχω αλλάξει σε Gdp
        // σχόλια δεν υπάρχουν, αφού είναι το ίδιο ακριβώς με το παραπάνω
        try {
            objResultGDP = stringToJasonObject(urlGDP);

            countryNameForGdpData.setText(objResultGDP.getAsJsonObject("dataset").get("name").toString().substring(21,objResultGDP.getAsJsonObject("dataset").get("name").toString().length()-1).toUpperCase());
            startDateGdp.setText(ldy + objResultGDP.getAsJsonObject("dataset").get("start_date").toString().substring(1, 5));
            endDateGdp.setText(ldy + objResultGDP.getAsJsonObject("dataset").get("end_date").toString().substring(1, 5));

            JsonArray jaGDP = objResultGDP.getAsJsonObject("dataset").get("data").getAsJsonArray();
            DefaultTableModel tableDataModelGDP = new DefaultTableModel();
            tableDataModelGDP.setColumnIdentifiers(new String[]{"Year", "Value"});

            for (int i = 0; i < jaGDP.size(); i++) {
                String str = jaGDP.get(i).toString();
                String year = str.substring(2, 6);
                String dataGDP = str.substring(14, str.length() - 1);
                tableDataModelGDP.addRow(new String[]{year, dataGDP});
            } // end for

            tableDataGDP.setModel(tableDataModelGDP);

        } catch (Exception e) {
            cleanDataFromInterface("gdp");
            JOptionPane.showMessageDialog(null, "There is no GDP data for this country ", "INFO", JOptionPane.INFORMATION_MESSAGE);
        }
    }// end void

// void για να πάρουμε δεδομένα τα οποία βρίσκονται στη βάση μας. 
    public void GetDataFromBase(String isoCodeFind) {

    // Στο table COUNTRY_DATASET το CountryCode είναι τύπου Country
    // Αυτό σημαίνει ότι για να κάνουμε αναζήτηση στο συγκεκριμένο table
    // θα πρέπει να έχουμε ένα αντικείμενο τύπου Country με ISO_CODE τον 3ψήφιο κωδικό
    // και ΝΑΜΕ το όνομα της χώρας
    Country selectedCountry = cjc.findCountry(isoCodeFind);

 Integer oilID = 0;  
 Integer gdpID = 0;  
 
    List<CountryDataset> countryDataset = cdsc.findCountryDatasetEntities();
        for (int i = 0; i < countryDataset.size(); i++) {
            if (countryDataset.get(i).getCountryCode().equals(selectedCountry)) {
                if (countryDataset.get(i).getDescription().contains("Oil") || countryDataset.get(i).getDescription().contains("OIL")) {
                    oilID = countryDataset.get(i).getDatasetId();

                    // TableModel είναι ο χώρος όπου θα μπουν τα αποτελέσματα της κατανάλωσης
                    // Πρέπει πρώτα να φτιάξουμε ένα αντικείμενο της κλάσης DefaultTableModel
                    DefaultTableModel tableDataModelOIL = new DefaultTableModel();

                    // Θέλουμε να εμφανίζει όνομα σε κάθε στήλη. Αν δεν το κάνουμε δεν θα έχουμε επικεφαλίδα
                    tableDataModelOIL.setColumnIdentifiers(new String[]{"Year", "Value"});

                    countryNameForOilData.setText(countryDataset.get(i).getName());
                    startDateOil.setText(ldy + countryDataset.get(i).getStartYear());
                    endDateOil.setText(ldy + countryDataset.get(i).getEndYear());

                } // end if OIL
                   
                if (countryDataset.get(i).getDescription().contains("GDP") || countryDataset.get(i).getDescription().contains("gdp")) {
                    gdpID = countryDataset.get(i).getDatasetId();

                    // TableModel είναι ο χώρος όπου θα μπουν τα αποτελέσματα της κατανάλωσης
                    // Πρέπει πρώτα να φτιάξουμε ένα αντικείμενο της κλάσης DefaultTableModel
                    DefaultTableModel tableDataModelGDP = new DefaultTableModel();

                    // Θέλουμε να εμφανίζει όνομα σε κάθε στήλη. Αν δεν το κάνουμε δεν θα έχουμε επικεφαλίδα
                    tableDataModelGDP.setColumnIdentifiers(new String[]{"Year", "Value"});

                    countryNameForGdpData.setText(countryDataset.get(i).getName());
                    startDateGdp.setText(ldy + countryDataset.get(i).getStartYear());
                    endDateGdp.setText(ldy + countryDataset.get(i).getEndYear());

                }// end if GDP

            }//end if
        }//end for
        
        
         DefaultTableModel tableDataModelOIL2 = new DefaultTableModel();

            // Θέλουμε να εμφανίζει όνομα σε κάθε στήλη. Αν δεν το κάνουμε δεν θα έχουμε επικεφαλίδα
            tableDataModelOIL2.setColumnIdentifiers(new String[]{"Year", "Value"});
            
            DefaultTableModel tableDataModelGDP2 = new DefaultTableModel();

            // Θέλουμε να εμφανίζει όνομα σε κάθε στήλη. Αν δεν το κάνουμε δεν θα έχουμε επικεφαλίδα
            tableDataModelGDP2.setColumnIdentifiers(new String[]{"Year", "Value"});
    
        List<CountryData> countryData = cdc.findCountryDataEntities();
        for (int i = 0; i < countryData.size(); i++) {
            if (Objects.equals(countryData.get(i).getDataset().getDatasetId(), oilID)) {
                
                String yearOIL = countryData.get(i).getDataYear();
                String dataOIL = countryData.get(i).getValue();
                // Συνθέτουμε τα δεδομένα στο modeλ. Σε κάθε γραμμή βάζουμε έτος και κατανάλωση
                tableDataModelOIL2.addRow(new String[]{yearOIL, dataOIL});
                
            }// end if Oil Data
            

            if (Objects.equals(countryData.get(i).getDataset().getDatasetId(), gdpID)) {
                
                String yearGDP = countryData.get(i).getDataYear();
                String dataGDP = countryData.get(i).getValue();
                // Συνθέτουμε τα δεδομένα στο modeλ. Σε κάθε γραμμή βάζουμε έτος και κατανάλωση
                tableDataModelGDP2.addRow(new String[]{yearGDP, dataGDP});
                
            }// end if GDP Data
            
        }
        tableDataOIL.setModel(tableDataModelOIL2);
        tableDataGDP.setModel(tableDataModelGDP2);
        // Αυτό είναι για να καθαρίζει το table που δεν έχει data
        if (oilID==0){cleanDataFromInterface("oil"); }
        if (gdpID==0){cleanDataFromInterface("gdp"); }
        save.setEnabled(false);
        
    }// end void
    
    void cleanDataFromInterface(String s) {
        if (null == s) {
            JOptionPane.showMessageDialog(null, "Wrong Input", "ERROR", JOptionPane.ERROR_MESSAGE);
        }//end if
 else   switch (s) {
            case "all":
                {
                    // Καθαρίζουμε το περιβάλλον από τα προηγούμενα δεδομένα
                    DefaultTableModel tableDataModel = new DefaultTableModel();
                    // Βάζουμε τίτλο στις στήλες, γιατί τους διαγράφει.
                    tableDataModel.setColumnIdentifiers(new String[]{"Year", "Value"});
                    for (int i = 0; i < 15; i++) {
                        tableDataModel.addRow(new String[]{null, null});
                    }//end for
                    tableDataGDP.setModel(tableDataModel);
                    tableDataOIL.setModel(tableDataModel);
                    countryNameForOilData.setText(" ");
                    countryNameForGdpData.setText(" ");
                    startDateGdp.setText(" ");
                    startDateOil.setText(" ");
                    endDateGdp.setText(" ");
                    endDateOil.setText(" ");
                    break;
                }
            case "oil":
                {
                    DefaultTableModel tableDataModel = new DefaultTableModel();
                    // Βάζουμε τίτλο στις στήλες, γιατί τους διαγράφει.
                    tableDataModel.setColumnIdentifiers(new String[]{"Year", "Value"});
                    for (int i = 0; i < 15; i++) {
                        tableDataModel.addRow(new String[]{null, null});
                    }//end for
                    tableDataOIL.setModel(tableDataModel);
                    countryNameForOilData.setText(" no data ");
                    startDateOil.setText(" ");
                    endDateOil.setText(" ");
                    break;
                }
            case "gdp":
                {
                    DefaultTableModel tableDataModel = new DefaultTableModel();
                    // Βάζουμε τίτλο στις στήλες, γιατί τους διαγράφει.
                    tableDataModel.setColumnIdentifiers(new String[]{"Year", "Value"});
                    for (int i = 0; i < 15; i++) {
                        tableDataModel.addRow(new String[]{null, null});
                    }//end for
                    tableDataGDP.setModel(tableDataModel);
                    countryNameForGdpData.setText(" no data ");
                    startDateGdp.setText(" ");
                    endDateGdp.setText(" ");
                    break;
                }
            default:
                JOptionPane.showMessageDialog(null, "Wrong Input", "ERROR", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }//end void
    
    
    // Επιστρέφει την χώρα που έχουμε επιλέξει από το μενού    
    public Country getSelectedCountry(){
        
// Στη μεταβλητή selection βάζουμε την επιλεγμένη προς αναζήτηση χώρα.
        String selection = countrySelection.getSelectedItem().toString();

 // Φτιάχνουμε ένα νέο αντικείμενο country με την χώρα που ψάχνουμε.
       // H Country αποτελείται από τον κωδικό της χώρας isoCode και
       // το όνομα της χώρας name
       // Υπάρχει κατασκευαστής χωρίς παραμέτρους.          
            Country selectedCountry = new Country();

           
           // cjc είναι ο controller που επικοινωνεί με το table Country
       // Οι κλάσεις Country, CountryData και CountryDataset φτιάχτηκαν από το Netbeans
       // To Netbeans για κάθε μία από αυτές τις κλάσεις, φτιάχνει αυτόματα και controllers
       // Έχουμε δηλαδή 3 controllers, έναν για κάθε table.
       // είναι αντικείμενο της κλάσης CountryJpaController.java
       // Προσθέτει στη λίστα countries τις χώρες που διάβασε απο τo table Country
       List<Country> countries = cjc.findCountryEntities();
     
        // Αν κάνουμε εδώ System.out.println(countries);
        // θα μας ενφανίζει τις χώρες
        // Διατρτέχουμε την λίστα με τις χώρες, για να βρούμε το αντικείμενο που μας ενδιαφέρει
        // Όταν βρει την χώρα που επέλεξε ο χρήστης
        // δίνει τον κωδικό και το όνομα της χώρας που ψάχνουμε στο selectedCountry
        // Οπότε στο αντικείμενο selectedCountry έχουμε τα στοιχεία της χώρας που θέλουμε.
        countries.stream().filter((c) -> (selection.equals(c.getName()))).map((c) -> {
            selectedCountry.setIsoCode(c.getIsoCode());
            return c;
        }).forEachOrdered((c) -> {
            selectedCountry.setName(c.getName());
        }); // end if
        // end for
   
        return selectedCountry;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        countrySelection = new javax.swing.JComboBox<>();
        fetchData = new javax.swing.JButton();
        deleteAll = new javax.swing.JButton();
        save = new javax.swing.JButton();
        plot = new javax.swing.JButton();
        CheckBox = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        countryNameForOilData = new javax.swing.JLabel();
        countryNameForGdpData = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableDataOIL = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableDataGDP = new javax.swing.JTable();
        startDateOil = new javax.swing.JLabel();
        endDateOil = new javax.swing.JLabel();
        startDateGdp = new javax.swing.JLabel();
        endDateGdp = new javax.swing.JLabel();
        flags = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Econometrics");
        setLocation(new java.awt.Point(500, 150));
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel1.setText("Select Country:");

        countrySelection.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        countrySelection.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "            ---- Select Country from the List ----" }));
        countrySelection.setToolTipText("");
        countrySelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countrySelectionActionPerformed(evt);
            }
        });

        fetchData.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        fetchData.setText("Fetch Data");
        fetchData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fetchDataActionPerformed(evt);
            }
        });

        deleteAll.setText("DELETE ALL");
        deleteAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAllActionPerformed(evt);
            }
        });

        save.setText("Save");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });

        plot.setText("Plot");
        plot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotActionPerformed(evt);
            }
        });

        CheckBox.setText("Already saved to Database");
        CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckBoxActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("OIL DATA");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel3.setText("Country:");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel4.setText("GDP DATA");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel5.setText("Country:");

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel6.setText("Available Timespan:");

        jLabel7.setText("Start Date:");

        jLabel8.setText("End Date:");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel9.setText("Available Timespan:");

        jLabel10.setText("Start Date:");

        jLabel11.setText("End Date:");

        tableDataOIL.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Year", "Value"
            }
        ));
        jScrollPane3.setViewportView(tableDataOIL);

        tableDataGDP.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Year", "Value"
            }
        ));
        jScrollPane2.setViewportView(tableDataGDP);
        if (tableDataGDP.getColumnModel().getColumnCount() > 0) {
            tableDataGDP.getColumnModel().getColumn(0).setHeaderValue("Year");
            tableDataGDP.getColumnModel().getColumn(1).setHeaderValue("Value");
        }

        flags.setText("   ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)
                            .addComponent(jLabel9)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(countryNameForOilData, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(startDateOil, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(endDateOil, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(save)
                                .addGap(28, 28, 28)
                                .addComponent(plot)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(deleteAll))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel5)
                                .addComponent(jLabel4)
                                .addComponent(jLabel6)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(10, 10, 10)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(18, 18, 18)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(endDateGdp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(startDateGdp, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)))
                                .addComponent(CheckBox, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(countryNameForGdpData, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(countrySelection, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(flags, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(49, 49, 49)
                        .addComponent(fetchData)))
                .addContainerGap(55, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(countrySelection, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fetchData, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(flags))
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel3))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(countryNameForOilData, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(countryNameForGdpData, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel7)
                    .addComponent(startDateOil)
                    .addComponent(startDateGdp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel8)
                    .addComponent(endDateOil)
                    .addComponent(endDateGdp))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deleteAll, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(plot, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(save, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void countrySelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countrySelectionActionPerformed
       cleanDataFromInterface("all");
  
       Country selectedCountry = new Country();
       // η getSelectedCountry() επιστρέφει την επιλεγεμένη χώρα
       selectedCountry = getSelectedCountry();

       // Από το αντικείμενο μου μόλις βρήκαμε, θέλουμε το ISOcode.
       // Το οποίο θα το πάρουμε από την αναζήτηση που κάναμε πιο πάνω.
       String isoCodeFind = selectedCountry.getIsoCode();
       

       // Τρέχουμε ένα sql query για να δούμε αν τα δεδομένα της χώρα είναι ήδη στη βάση μας.
       // To query είναι έτοιμο και αυτό από το Netbeans στην κλάση COUNTRY_DATASET
       // Ψάχνουμε στο table CountryDataset, γιατί εκεί αποθηκεύονται τα δεδομένα που μας ενδιαφέρουν.
       // Ψάχνουμε εκεί, ώστε αν υπάρχουν ήδη τα δεδομένα της χώρας να μην ψάχνουμε με το api
       Query query = em.createQuery("SELECT c FROM CountryDataset c WHERE c.countryCode = :name");
       query.setParameter("name", selectedCountry);

       // Φτιάχνουμε μία λίστα αντικειμένου CountryDataset.
       // Μέσα θα βάλουμε τα αποτελέσματα από την αναζήτηση στη βάση.
       List<CountryDataset> countryDataset = query.getResultList();

       // An εκτελέσoyme System.out.println(countryDataset); θα δείτε αν υπάρχουν δεδομένα της χώρας στο table CountryDataset. 
       // Αν δεν βρούμε την χώρα που ψάχνουμε στη βάση, τότε θα πρέπει να τα αναζητήσουμε ta δεδομένα στο ιντερνετ.
       if (countryDataset.isEmpty()) {
           //Αν δεν υπάρχει στη βάση, δηλαδή είναι άδεια η λίστα τότε αρχικά ενεργοποιούμε το κουμπί SAVE.
           // ΠΡΟΣΟΧΗ to table μπορεί να μην είναι άδειο, αλλά δεν υπάρχει η χώρα που θέλουμε
           // Το countryDataset είναι αντικείμενο της κλάσης CountryDataset και μέσα έχουμε βάλει το αποτέλεσμα της αναζήτησης
           // Αν είναι άδειο δε σημαίνει ότι το table είναι άδειο.
           CheckBox.setSelected(false);
           save.setEnabled(true);
           
              
           
         
               
           
       

       } else {
           // αν όμως υπάρχουν ήδη τα δεδομένα, τότε τα παίρνουμε από την βάση
           // Η μέθοδος GetDataFromBase έχει παράμετρο τον κωδικό της χώρας.
           CheckBox.setSelected(true);
           save.setEnabled(false);
       }

       try {
           // Βάζουμε τη σημαία της χώρας    
           flags.setIcon(new javax.swing.ImageIcon(getClass().getResource("/flags/" + isoCodeFind + ".png"))); 
       } catch (Exception exce) {
           // Αν δεν υπάρχει η σημαία της χώρας τότε βάζουμε τη σημαία της ΓΗΣ
           flags.setIcon(new javax.swing.ImageIcon(getClass().getResource("/flags/NOFLAG.png")));
       }

   }//GEN-LAST:event_countrySelectionActionPerformed

    private void fetchDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fetchDataActionPerformed
  
        // Μόλις πατηθεί το fetch τότε ενεργοπιείται και το plot
        plot.setEnabled(true);
        CheckBox.setSelected(false);
        // Δίνουμε action στο κουμπί fetch Data
        try {
            
            Country selectedCountry = new Country();
            // η getSelectedCountry() επιστρέφει την επιλεγεμένη χώρα
            selectedCountry = getSelectedCountry();

            // Από το αντικείμενο μου μόλις βρήκαμε, θέλουμε το ISOcode.
            // Το οποίο θα το πάρουμε από την αναζήτηση που κάναμε πιο πάνω.
            String isoCodeFind = selectedCountry.getIsoCode();
            

            // Τρέχουμε ένα sql query για να δούμε αν τα δεδομένα της χώρα είναι ήδη στη βάση μας.
            // To query είναι έτοιμο και αυτό από το Netbeans στην κλάση COUNTRY_DATASET
            // Ψάχνουμε στο table CountryDataset, γιατί εκεί αποθηκεύονται τα δεδομένα που μας ενδιαφέρουν.
            // Ψάχνουμε εκεί, ώστε αν υπάρχουν ήδη τα δεδομένα της χώρας να μην ψάχνουμε με το api
            Query query = em.createQuery("SELECT c FROM CountryDataset c WHERE c.countryCode = :name");
            query.setParameter("name", selectedCountry);

            // Φτιάχνουμε μία λίστα αντικειμένου CountryDataset.
            // Μέσα θα βάλουμε τα αποτελέσματα από την αναζήτηση στη βάση.
            List<CountryDataset> countryDataset = query.getResultList();

            // An εκτελέσoyme System.out.println(countryDataset); θα δείτε αν υπάρχουν δεδομένα της χώρας στο table CountryDataset. 
            // Αν δεν βρούμε την χώρα που ψάχνουμε στη βάση, τότε θα πρέπει να τα αναζητήσουμε ta δεδομένα στο ιντερνετ.
            if (countryDataset.isEmpty()) {
                //Αν δεν υπάρχει στη βάση, δηλαδή είναι άδεια η λίστα τότε αρχικά ενεργοποιούμε το κουμπί SAVE.
                // ΠΡΟΣΟΧΗ to table μπορεί να μην είναι άδειο, αλλά δεν υπάρχει η χώρα που θέλουμε
                // Το countryDataset είναι αντικείμενο της κλάσης CountryDataset και μέσα έχουμε βάλει το αποτέλεσμα της αναζήτησης
                // Αν είναι άδειο δε σημαίνει ότι το table είναι άδειο.
                save.setEnabled(true);

                // Καλούμε την μέδοδο GetJson η οποία έχει σαν παράμετρο τον κωδικό της χώρα
                JsonToJava(isoCodeFind);
            } else {
                // αν όμως υπάρχουν ήδη τα δεδομένα, τότε τα παίρνουμε από την βάση
                // Η μέθοδος GetDataFromBase έχει παράμετρο τον κωδικό της χώρας.
                CheckBox.setSelected(true);
                GetDataFromBase(isoCodeFind);
            }

        } catch (IOException e) {
            Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, e);

        } catch (Exception ex) {
            Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_fetchDataActionPerformed

    private void plotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotActionPerformed
        
        // Θέλουμε να πάρουμε το ISO code της χώρας
        // Κάνουμε ότι κάναμε στο save, στο fetch
        
        // Φτιάχνουμε αντικείμενο της κλάσης Country
        // Και βάζουμε μέσα την επιλεγμένη χώρα
        // Μετά θα περάσουμε αυτή τη χώρα ως παράμετρο
        Country selectedCountry;
        selectedCountry = getSelectedCountry();
        
        try {
            // ****************************************************************************
            // * JFREECHART DEVELOPER GUIDE                                               *
            // * The JFreeChart Developer Guide, written by David Gilbert, is available   *
            // * to purchase from Object Refinery Limited:                                *
            // *                                                                          *
            // * http://www.object-refinery.com/jfreechart/guide.html                     *
            // *                                                                          *
            // * Sales are used to       funding for the JFreeChart project - please    *
            // * support us so that we can continue developing free software.             *
            // ****************************************************************************
            /**
             * Starting point for the demonstration application.
             *
             * @param args ignored.
             */
            
            // Αυτό το κομμάτι είναι από το http://www.java2s.com/Code/Java/Chart/JFreeChartDualAxisDemo2.htm
            //Δημιουργώ ένα αντικείμενο για διάγραμμα και του περνάω ως όρισμα String και Χώρα
            

            
            PlotDiagram dataPlot = new PlotDiagram("Economic Data", selectedCountry);
            //Δημιουργώ το παράθυρο με το διάγραμμα
            dataPlot.pack();
            //Τοποθετώ το παράθυρο στο κέντρο
            RefineryUtilities.centerFrameOnScreen(dataPlot);
            //Το εμφανίζω
            dataPlot.setVisible(true);
        } catch (MalformedURLException ex) {
            Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_plotActionPerformed

    private void deleteAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAllActionPerformed
      int deleteConfirmimation=JOptionPane.showConfirmDialog(null,"This action will erase ALL the Data from the DB\n\n"
                + "Are you sure you want to procceed?","Warning Message",
                JOptionPane.YES_NO_OPTION);
      if (deleteConfirmimation==0){  
        try{
            em.getTransaction().begin();
           
            // ΠΡΟΣΟΧΗ, έχει σημασία με ποια σειρά διαγράφουμε τα tables.
            // Πρώτα το CountryData γιατί δεν δίνει δεδομένα σε κάποια table 
            em.createQuery("DELETE FROM CountryData").executeUpdate();
            // Μετά το CountryDataset το οποίο δίνει δεδομένα (DATASET) στο table CountryData
            em.createQuery("DELETE FROM CountryDataset").executeUpdate();
            // Τέλος το Country του οποίο το ISO_CODE είναι κλειδία σε άλλο table
            em.createQuery("DELETE FROM Country").executeUpdate();
            em.getTransaction().commit();
            
            JOptionPane.showMessageDialog(null, "The Database has been deleted", "INFO", JOptionPane.INFORMATION_MESSAGE);
            // η παράμετρος all σημαίνει ότι θα καθαρίσει τα data και από τα δύο τραπέζια (GDP, OIL)
            cleanDataFromInterface("all");
          
           csvDataToDropDownMenu();    
        }catch(Exception e){
              JOptionPane.showMessageDialog(null, "IOEcxeption has been caught ", "INFO", JOptionPane.INFORMATION_MESSAGE);
        }
      }
    }//GEN-LAST:event_deleteAllActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed

     
        
        
        
        
        
        
        
        
        
        
        
        
        // Στη μεταβλητή selection βάζουμε την επιλεγμένη προς αναζήτηση χώρα.
        // Ακριβώς ότι κάναμε και στο fetch
        // Ο κώδικας είναι ίδιος
        // Αν υπάρχει ήδη στη βάση, δεν θα ξανακάνει save
        if (CheckBox.isSelected() == false){

        Country selectedCountry = new Country();
        // Η getSelectedCountry() επιστρέφει την χώρα που έχει επιλεγεί από το μενού 
        selectedCountry = getSelectedCountry();
        // Παίρνουμε το ISOcode.
        String isoCodeFind = selectedCountry.getIsoCode();

        // Φτιάχνουμε αντικείμενα JsonObject για να βάζουμε τα αποτελέσματα.
        JsonObject objResultOIL = null;
        JsonObject objResultGDP = null;

        // Πρώτα θα σχοληθούμε με τα δεδομένα για το OIL
        // Όπως και στο fetch
        try {
            URL urlOIL = new URL("https://www.quandl.com/api/v3/datasets/BP/OIL_CONSUM_" + isoCodeFind + ".json?api_key=" + key);

            // Αυτό το χρησιμοποιούμε για την περίπτωση την οποία για μία χώρα υπάρχουν δεδομένα μόνο για OIL ή GDP
            // Θέλουμε να δούμε αν υπάρχουν δεδομένα στο request. Αν δεν υπάρχουν, δεν έχουμε να αποθηκεύσουμε κάτι.
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(urlOIL).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    Gson gson = new Gson();
                    JsonElement element = gson.fromJson(responseString, JsonElement.class);
                    objResultOIL = element.getAsJsonObject();

                    // Φτιάχνουμε ένα νέο αντικείμενο countryData και ένα countryDataset με την χώρα που ψάχνουμε.
                    // Υπάρχει κατασκευαστής χωρίς παραμέτρους.
                    CountryData selectedCountryData = new CountryData();
                    CountryDataset selectedCountryDataset = new CountryDataset();

                    // Εδώ θέτουμε τιμές στο αντικείμενο τύπου CountryDataset
                    // Ξεκινάμε από τα αντικείμενα CountryDataset γιατί θέλουμε να τα ανεβάσουμε στη βάση για να πάρουμε το κλειδί (DATASET_ID)
                    selectedCountryDataset.setCountryCode(selectedCountry);
                    selectedCountryDataset.setName(objResultOIL.getAsJsonObject("dataset").get("name").toString().substring(19, objResultOIL.getAsJsonObject("dataset").get("name").toString().length() - 1).toUpperCase());
                    selectedCountryDataset.setDescription(objResultOIL.getAsJsonObject("dataset").get("description").toString());
                    selectedCountryDataset.setStartYear(objResultOIL.getAsJsonObject("dataset").get("start_date").toString().substring(1, 5));
                    selectedCountryDataset.setEndYear(objResultOIL.getAsJsonObject("dataset").get("end_date").toString().substring(1, 5));

                    cdsc.create(selectedCountryDataset);

                    // εδώ φτιάχνουμε πίνακα, γιατί τα δεδομένα για την κατανάλωση είναι σε πίνακα
                    // ότι κάναμε και στο fetch μόνο που τώρα δεν θα τα εμφανίσουμε στην οθόνη αλλά θα τα ανεβάσουμε στη βάση 
                    JsonArray jaOIL = objResultOIL.getAsJsonObject("dataset").get("data").getAsJsonArray();

                    // Το jaOIL είναι ο πίνακας που έχει τα δεδομένα που θέλουμε να αποθηκεύσουμε
                    // Διατρέχουμε τον πίνακα και σε κάθε γραμμή θα βάζουμε το έτος και δίπλα την κατανάλωση
                    for (int i = 0; i < jaOIL.size(); i++) {
                        // Στην year βάζουμε το έτος, μόνο το έτος γι'αυτό κόβουμε ότι περισσεύει
                        // Βάζουμε σε μία μεταβλητή τύπου String το περιεχόμενο του πίνακα της θέσης i
                        // Το συγκερκιμένο string είναι κάπως έτσι "["2018-12-31",15.735100014]"
                        String str = jaOIL.get(i).toString();
                        // Για το πάρουμε το έτος πετάμε τους δύο πρώτους χαρακτήρες και κρατάμε τους υπόλοιπους 4
                        String year = str.substring(2, 6);
                        // Για να πάρουμε την κατανάλωση αρχίζουμε από τον 14ο χαρακτήρα μέχρι τον προτελευταίο
                        String dataOIL = str.substring(14, str.length() - 1);

                        selectedCountryData.setDataYear(year);
                        selectedCountryData.setValue(dataOIL);
                        selectedCountryData.setDataset(selectedCountryDataset);
                        // Τα ανεβάζουμε στη βάση
                        cdc.create(selectedCountryData);
                    } // end for
                } else {
                    // Εμφάνιση μηνύματος ότι δεν υπάρχουν τα συγκεκριμένα data
                    cleanDataFromInterface("oil");
                    JOptionPane.showMessageDialog(null, "There is NO OIL data to be saved", "INFO", JOptionPane.INFORMATION_MESSAGE);
                }//end else
            } catch (IOException e) {
                Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, e);
            }
        } catch (MalformedURLException ex) {
          JOptionPane.showMessageDialog(null, "Unreachable host", "ERROR", JOptionPane.ERROR_MESSAGE);
        }

        // για GDP
        try {
            // είναι ίδιο με το OIL
            URL urlGDP = new URL("https://www.quandl.com/api/v3/datasets/WWDI/" + isoCodeFind + "_NY_GDP_MKTP_CN.json?api_key=" + key);
            objResultGDP = stringToJasonObject(urlGDP);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(urlGDP).build();

            // Όταν δεν υπάρχουν δεδομένα να αποθηκεύσει θα ενφανιστεί ένα μήνυμα.
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    Gson gson = new Gson();
                    JsonElement element = gson.fromJson(responseString, JsonElement.class);
                    objResultGDP = element.getAsJsonObject();

                    // Φτιάχνουμε ένα νέο αντικείμενο countryData και ένα countryDataset με την χώρα που ψάχνουμε.
                    // Υπάρχει κατασκευαστής χωρίς παραμέτρους.
                    CountryData selectedCountryData = new CountryData();
                    CountryDataset selectedCountryDataset = new CountryDataset();

                    // Εδώ θέτουμε τιμές στο αντικείμενο τύπου CountryDataset
                    // Ξεκινάμε από τα αντικείμενα CountryDataset γιατί θέλουμε να τα ανεβάσουμε στη βάση για να πάρουμε το κλειδί (DATASET_ID)
                    selectedCountryDataset.setCountryCode(selectedCountry);
                    selectedCountryDataset.setName(objResultGDP.getAsJsonObject("dataset").get("name").toString().substring(21, objResultGDP.getAsJsonObject("dataset").get("name").toString().length() - 1).toUpperCase());
                    selectedCountryDataset.setDescription(objResultGDP.getAsJsonObject("dataset").get("description").toString());
                    selectedCountryDataset.setStartYear(objResultGDP.getAsJsonObject("dataset").get("start_date").toString().substring(1, 5));
                    selectedCountryDataset.setEndYear(objResultGDP.getAsJsonObject("dataset").get("end_date").toString().substring(1, 5));

                    cdsc.create(selectedCountryDataset);

                    JsonArray jaGDP = objResultGDP.getAsJsonObject("dataset").get("data").getAsJsonArray();
                    
                    for (int i = 0; i < jaGDP.size(); i++) {
                        // Στην year βάζουμε το έτος, μόνο το έτος γι'αυτό κόβουμε ότι περισσεύει
                        // Βάζουμε σε μία μεταβλητή τύπου String το περιεχόμενο του πίνακα της θέσης i
                        // Το συγκερκιμένο string είναι κάπως έτσι "["2018-12-31",15.735100014]"
                        String str = jaGDP.get(i).toString();
                        // Για το πάρουμε το έτος πετάμε τους δύο πρώτους χαρακτήρες και κρατάμε τους υπόλοιπους 4
                        String year = str.substring(2, 6);
                        // Για να πάρουμε την κατανάλωση αρχίζουμε από τον 14ο χαρακτήρα μέχρι τον προτελευταίο
                        String dataGDP = str.substring(14, str.length() - 1);

                        selectedCountryData.setDataYear(year);
                        selectedCountryData.setValue(dataGDP);
                        selectedCountryData.setDataset(selectedCountryDataset);
                        // Τα ανεβάζουμε στη βάση
                        cdc.create(selectedCountryData);
                    } // end for

                } else {
                    JOptionPane.showMessageDialog(null, "There is no GDP data to be saved ", "INFO", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, e);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
        }else{
            JOptionPane.showMessageDialog(null, "data has already been saved ", "INFO", JOptionPane.INFORMATION_MESSAGE);
        }
        // Κάνουμε Selected το κουτάκι
        CheckBox.setSelected(true);
        save.setEnabled(false);
        
        
    }//GEN-LAST:event_saveActionPerformed

    private void CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckBoxActionPerformed
        
    }//GEN-LAST:event_CheckBoxActionPerformed

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      /* Set the Nimbus look and feel */
      //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
      /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
       */
      try {
         for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
               javax.swing.UIManager.setLookAndFeel(info.getClassName());
               break;
            }
         }
      } catch (ClassNotFoundException ex) {
         java.util.logging.Logger.getLogger(EconometricsInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (InstantiationException ex) {
         java.util.logging.Logger.getLogger(EconometricsInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
         java.util.logging.Logger.getLogger(EconometricsInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (javax.swing.UnsupportedLookAndFeelException ex) {
         java.util.logging.Logger.getLogger(EconometricsInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      }
      //</editor-fold>
      
      
      /* Create and display the form */
      java.awt.EventQueue.invokeLater(() -> {
          try {
              new EconometricsInterface().setVisible(true);
          } catch (Exception ex) {
              Logger.getLogger(EconometricsInterface.class.getName()).log(Level.SEVERE, null, ex);
          }
      });
   }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox CheckBox;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel countryNameForGdpData;
    private javax.swing.JLabel countryNameForOilData;
    private javax.swing.JComboBox<String> countrySelection;
    private javax.swing.JButton deleteAll;
    private javax.swing.JLabel endDateGdp;
    private javax.swing.JLabel endDateOil;
    private javax.swing.JButton fetchData;
    private javax.swing.JLabel flags;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton plot;
    private javax.swing.JButton save;
    private javax.swing.JLabel startDateGdp;
    private javax.swing.JLabel startDateOil;
    private javax.swing.JTable tableDataGDP;
    private javax.swing.JTable tableDataOIL;
    // End of variables declaration//GEN-END:variables

    
}
