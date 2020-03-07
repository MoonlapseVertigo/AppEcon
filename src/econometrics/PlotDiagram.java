/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 * ------------------
 * DualAxisDemo2.java
 * ------------------
 * (C) Copyright 2002-2004, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * $Id: DualAxisDemo2.java,v 1.17 2004/05/21 10:09:10 mungady Exp $
 *
 * Changes
 * -------
 * 19-Nov-2002 : Version 1 (DG);
 * 27-Apr-2004 : Updated for changes to the XYPlot class (DG);
 *
 */

package econometrics;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ecoDB.Country;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.StandardChartTheme;

/**
 * An example of a time series chart.  For the most part, default settings are used, except that
 * the renderer is modified to show filled shapes (as well as lines) at each data point.
 *
 */


/**
 *
 * @author moonlapsevertigo
 */
 

public class PlotDiagram extends JFrame {
// Εδώ ΠΡΟΣΟΧΗ αν βάλουμε extends ApplicationFrame τότε όταν το κλείνουμε θα κλείνει και το app
// εμείς θέλουμε να κλείνει μόνο το frame.   
    
    public final String key = "1yq6zx7pcAS-x26YRA5M"; //api key
    
    public PlotDiagram (final String title, Country country) throws MalformedURLException {
        

        super(title);

        // create a title...
        final String chartTitle = "Economic Data for " + country.getName() + " ["+ country.getIsoCode()+ "]";
        final XYDataset dataset = createDatasetGDP(country);

        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
            chartTitle, 
            "Year", 
            "GDP",
            dataset, 
            true, 
            true, 
            false
        );
 
  //      final StandardLegend legend = (StandardLegend) chart.getLegend();
    //    legend.setDisplaySeriesShapes(true);
        
        final XYPlot plot = chart.getXYPlot();
        final NumberAxis axis2 = new NumberAxis(("Oil "));
        axis2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, createDatasetOIL(country));
        plot.mapDatasetToRangeAxis(1, 1);
        final XYItemRenderer renderer = plot.getRenderer();
        renderer.setToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
        if (renderer instanceof StandardXYItemRenderer) {
            final StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
            rr.setPlotLines(true);
            rr.setShapesFilled(true);
        }
        
        final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        renderer2.setSeriesPaint(0, Color.blue);
        renderer2.setPlotLines(true);
        renderer.setToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
        plot.setRenderer(1, renderer2);
        
        //Μετατρέπουμε την αριστερή μεριά του διαγράμματος σε εκατομμύρια.
        NumberAxis convertToMillions = (NumberAxis) plot.getRangeAxis();
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        convertToMillions.setNumberFormatOverride(formatter);
        
        final DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy"));
        
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 600));
        setContentPane(chartPanel);

    }

    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private XYDataset createDatasetGDP(Country c) {

        final TimeSeries s1 = new TimeSeries("GDP (current LCU)", Year.class);
        JsonObject objResultGDP = null;
        String isoCodeFind = c.getIsoCode();
        try {
            URL urlGDP = new URL("https://www.quandl.com/api/v3/datasets/WWDI/" + isoCodeFind + "_NY_GDP_MKTP_CN.json?api_key=" + key);
            objResultGDP = stringToJasonObject(urlGDP);
            if (objResultGDP == null) {
                final TimeSeriesCollection dataset = new TimeSeriesCollection();
                return dataset;
            } else {
                JsonArray jaGDP = objResultGDP.getAsJsonObject("dataset").get("data").getAsJsonArray();
                for (int i = 0; i < jaGDP.size(); i++) {
                    String str = jaGDP.get(i).toString();
                    int year = Integer.parseInt(str.substring(2, 6));
                    BigDecimal value = new BigDecimal(str.substring(14, str.length() - 1));
                    s1.add(new Year(year), value);
                }
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(PlotDiagram.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);

        return dataset;

    }

    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private XYDataset createDatasetOIL(Country c) {
        
        final TimeSeries s2 = new TimeSeries("Oil Consumption", Year.class);
        JsonObject objResultOIL = null;
        String isoCodeFind = c.getIsoCode();

        try {
            URL urlOIL = new URL("https://www.quandl.com/api/v3/datasets/BP/OIL_CONSUM_" + isoCodeFind + ".json?api_key=" + key);
            objResultOIL = stringToJasonObject(urlOIL);
            if (objResultOIL == null) {
                final TimeSeriesCollection dataset = new TimeSeriesCollection();
                return dataset;
            } else {
                JsonArray jaOIL = objResultOIL.getAsJsonObject("dataset").get("data").getAsJsonArray();
                for (int i = 0; i < jaOIL.size(); i++) {
                    String str = jaOIL.get(i).toString();
                    int year = Integer.parseInt(str.substring(2, 6));
                    double value = Double.parseDouble(str.substring(14, str.length() - 1));
                    s2.add(new Year(year), value * 1000000);
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(PlotDiagram.class.getName()).log(Level.SEVERE, null, ex);
        }

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s2);

        return dataset;

    
    }
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
             }else{return null;}
         }catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObj;
    }

}
