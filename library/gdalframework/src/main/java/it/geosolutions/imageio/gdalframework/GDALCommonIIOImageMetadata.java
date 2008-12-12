/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    https://imageio-ext.dev.java.net/
 *    (C) 2007 - 2008, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageio.gdalframework;

import it.geosolutions.imageio.core.CoreCommonImageMetadata;
import it.geosolutions.imageio.core.GCP;

import java.awt.Dimension;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.w3c.dom.Node;

/**
 * Class needed to store all available information of a GDAL Dataset with the
 * add of additional information. For convenience and future re-use this class
 * also represents an {@link IIOMetadata}. A wide set of getters method allow
 * to retrieve several information directly from the metadata instance, without
 * need of getting the XML DOM nodes tree.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * @author Daniele Romagnoli, GeoSolutions.
 */
public class GDALCommonIIOImageMetadata extends CoreCommonImageMetadata {

    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger
            .getLogger("it.geosolutions.imageio.gdalframework");
    
    private class GDALGCP implements GCP{
        private final org.gdal.gdal.GCP gcp;
        
        public GDALGCP(final org.gdal.gdal.GCP gcp){
            if (gcp==null)
                throw new NullPointerException("provided GCP is null");
            this.gcp=gcp;
        }

        public double getGCPLine() {
            return gcp.getGCPLine();
        }

        public double getGCPPixel() {
            return gcp.getGCPPixel();
        }

        public double getGCPX() {
            return gcp.getGCPX();
        }

        public double getGCPY() {
            return gcp.getGCPY();
        }

        public double getGCPZ() {
            return gcp.getGCPZ();
        }

        public String getId() {
            return gcp.getId();
        }

        public String getInfo() {
            return gcp.getInfo();
        }
        
    }

    /**
     * A map containing an HashMap for each domain if available (the Default
     * domain, the ImageStructure domain, as well as any xml prefixed domain)
     */
    protected Map gdalMetadataMap;

    /**
     * <code>GDALCommonIIOImageMetadata</code> constructor. Firstly, it
     * provides to open a dataset from the specified input dataset name. Then,
     * it call the constructor which initializes all fields with dataset
     * properties, such as raster size, raster tiling properties, projection,
     * and more.
     * 
     * @param sDatasetName
     *                The name (usually a File path or a subdataset name when
     *                the format supports subdatasets) of the dataset we want to
     *                open.
     */
    public GDALCommonIIOImageMetadata(String sDatasetName) {
        this(sDatasetName, nativeMetadataFormatName,
                nativeMetadataFormatClassName);
    }

    /**
     * <code>GDALCommonIIOImageMetadata</code> constructor. Firstly, it
     * provides to open a dataset from the specified input dataset name. Then,
     * it call the constructor which initializes all fields with dataset
     * properties, such as raster size, raster tiling properties, projection,
     * and more.
     * 
     * @param sDatasetName
     *                The name (usually a File path or a subdataset name when
     *                the format supports subdatasets) of the dataset we want to
     *                open.
     * @param formatName
     *                the name of the native metadata format
     * @param formatClassName
     *                the name of the class of the native metadata format
     */
    public GDALCommonIIOImageMetadata(String sDatasetName, String formatName,
            String formatClassName) {
        this(GDALUtilities.acquireDataSet(sDatasetName, gdalconst.GA_ReadOnly),
                sDatasetName, formatName, formatClassName);
    }

    /**
     * <code>GDALCommonIIOImageMetadata</code> constructor.
     * 
     * @param dataset
     *                the input <code>Dataset</code> on which build the common
     *                metadata object.
     * @param name
     *                the name to be set for the dataset represented by this
     *                common metadata object.
     * @param initializationRequired
     *                specify if initializing fields is required or not.
     * @param formatName
     *                the name of the native metadata format
     * @param formatClassName
     *                the name of the class of the native metadata format
     */
    public GDALCommonIIOImageMetadata(Dataset dataset, String name,
            final boolean initializationRequired, final String formatName,
            final String formatClassName) {
        super(false, formatName, formatClassName, null, null);
        setDatasetName(name);
        if (dataset == null)
            return;
        setDatasetDescription(dataset.GetDescription());
        Driver driver = dataset.GetDriver();
        if (driver != null) {
            setDriverDescription(driver.GetDescription());
            setDriverName(driver.getShortName());
        }
        gdalMetadataMap = Collections.synchronizedMap(new HashMap(2));

        // //
        //
        // Getting Metadata from Default domain and Image_structure domain
        //
        // //
        Map defMap = dataset
                .GetMetadata_Dict(GDALUtilities.GDALMetadataDomain.DEFAULT);
        if (defMap != null && defMap.size() > 0)
            gdalMetadataMap.put(
                    GDALUtilities.GDALMetadataDomain.DEFAULT_KEY_MAP, defMap);

        Map imageStMap = dataset
                .GetMetadata_Dict(GDALUtilities.GDALMetadataDomain.IMAGESTRUCTURE);
        if (imageStMap != null && imageStMap.size() > 0)
            gdalMetadataMap
                    .put(GDALUtilities.GDALMetadataDomain.IMAGESTRUCTURE,
                            imageStMap);

        // //
        //
        // Initializing member if needed
        //
        // //
        if (initializationRequired)
            setMembers(dataset);
        setGeoreferencingInfo(dataset);
        // clean up data set in order to avoid keeping them around for a lot
        // of time.
        GDALUtilities.closeDataSet(dataset);
    }

    /**
     * Constructor which initializes fields by retrieving properties such as
     * raster size, raster tiling properties, projection, and more from a given
     * input <code>Dataset</code>.
     * 
     * @param dataset
     *                the <code>Dataset</code> used to initialize all the
     *                common metadata fields.
     * @param name
     *                the dataset name
     * @param formatName
     *                the name of the native metadata format
     * @param formatClassName
     *                the name of the class of the native metadata format
     */
    public GDALCommonIIOImageMetadata(Dataset dataset, String name,
            final String formatName, final String formatClassName) {
        this(dataset, name, true, formatName, formatClassName);
    }

    /**
     * Constructor which initializes fields by retrieving properties such as
     * raster size, raster tiling properties, projection, and more from a given
     * input <code>Dataset</code> if not null.
     * 
     * @param dataset
     *                the <code>Dataset</code> used to initialize all the
     *                common metadata fields.
     * @param name
     *                the dataset name
     * 
     */
    public GDALCommonIIOImageMetadata(Dataset dataset, String name,
            final boolean initializationRequired) {
        this(dataset, name, initializationRequired, nativeMetadataFormatName,
                nativeMetadataFormatClassName);
    }

    /**
     * Set georeferencing information from an input <code>Dataset</code>
     * 
     * @param dataset
     *                a <code>Dataset</code> from where to retrieve all
     *                georeferencing information available
     */
    private void setGeoreferencingInfo(Dataset dataset) {
        // Setting CRS's related information
        final double[] geoT = new double[6];
        dataset.GetGeoTransform(geoT);
        setGeoTransformation(geoT);
        setProjection(dataset.GetProjection());
        setGcpProjection(dataset.GetGCPProjection());
        setGcpNumber(dataset.GetGCPCount());
    }

    /**
     * Set all the fields of the common metadata object.
     * 
     * @param dataset
     *                the <code>Dataset</code> which will be used for the
     *                initialization
     * @return <code>true</code> if the initialization was successfully
     *         completed. <code>false</code> if some field wasn't properly
     *         initialized
     */
    private boolean setMembers(Dataset dataset) {
        // Retrieving raster properties
        setWidth(dataset.getRasterXSize());
        setHeight(dataset.getRasterYSize());

        // Retrieving block size
        final int[] xBlockSize = new int[1];
        final int[] yBlockSize = new int[1];

        // Remember: RasterBand numeration starts from 1
        dataset.GetRasterBand(1).GetBlockSize(xBlockSize, yBlockSize);

        final int tileHeight = yBlockSize[0];
        final int tileWidth = xBlockSize[0];
        setTileHeight(tileHeight);
        setTileWidth(tileWidth);
        if (((long) tileHeight) * ((long) tileWidth) > Integer.MAX_VALUE)
            performTileSizeTuning(dataset);

        // /////////////////////////////////////////////////////////////////
        //
        // Getting dataset main properties
        //
        // /////////////////////////////////////////////////////////////////
        final int numBands = dataset.getRasterCount();
        setNumBands(numBands);
        if (numBands <= 0)
            return false;
        // final int xsize = dataset.getRasterXSize();
        // final int ysize = dataset.getRasterYSize();

        // If the image is very big, its size expressed as the number of
        // bytes needed to store pixels, may be a negative number
        final int tileSize = tileWidth
                * tileHeight
                * numBands
                * (gdal.GetDataTypeSize(dataset.GetRasterBand(1).getDataType()) / 8);

        // bands variables
        final int[] banks = new int[numBands];
        final int[] offsetsR = new int[numBands];
        final Double[] noDataValues = new Double[numBands];
        final Double[] scales = new Double[numBands];
        final Double[] offsets = new Double[numBands];
        final Double[] minimums = new Double[numBands];
        final Double[] maximums = new Double[numBands];
        final int[] numOverviews = new int[numBands];
        final int[] colorInterpretations = new int[numBands];
        int buf_type = 0;

        Band pBand = null;

        // scanning bands
        final Double tempD[] = new Double[1];
        final int bandsOffset[] = new int[numBands];
        for (int band = 0; band < numBands; band++) {
            /* Bands are not 0-base indexed, so we must add 1 */
            pBand = dataset.GetRasterBand(band + 1);
            buf_type = pBand.getDataType();
            banks[band] = band;
            offsetsR[band] = 0;
            pBand.GetNoDataValue(tempD);
            noDataValues[band] = tempD[0];
            pBand.GetOffset(tempD);
            offsets[band] = tempD[0];
            pBand.GetScale(tempD);
            scales[band] = tempD[0];
            pBand.GetMinimum(tempD);
            minimums[band] = tempD[0];
            pBand.GetMaximum(tempD);
            maximums[band] = tempD[0];
            colorInterpretations[band] = pBand.GetRasterColorInterpretation();
            numOverviews[band] = pBand.GetOverviewCount();
            bandsOffset[band] = band;
        }
        setNoDataValues(noDataValues);
        setScales(scales);
        setOffsets(offsets);
        setMinimums(minimums);
        setMaximums(maximums);
        setNumOverviews(numOverviews);
        setColorInterpretations(colorInterpretations);

        // /////////////////////////////////////////////////////////////////
        //
        // Variable used to specify the data type for the storing samples
        // of the SampleModel
        //
        // /////////////////////////////////////////////////////////////////
        int buffer_type = 0;
        if (buf_type == gdalconstConstants.GDT_Byte)
            buffer_type = DataBuffer.TYPE_BYTE;
        else if (buf_type == gdalconstConstants.GDT_UInt16)
            buffer_type = DataBuffer.TYPE_USHORT;
        else if (buf_type == gdalconstConstants.GDT_Int16)
            buffer_type = DataBuffer.TYPE_SHORT;
        else if ((buf_type == gdalconstConstants.GDT_Int32)
                || (buf_type == gdalconstConstants.GDT_UInt32))
            buffer_type = DataBuffer.TYPE_INT;
        else if (buf_type == gdalconstConstants.GDT_Float32)
            buffer_type = DataBuffer.TYPE_FLOAT;
        else if (buf_type == gdalconstConstants.GDT_Float64)
            buffer_type = DataBuffer.TYPE_DOUBLE;
        else
            return false;

        // //
        //
        // Setting the Sample Model
        //
        // Here you have a nice trick. If you check the SampleMOdel class
        // you'll see that there is an actual limitation on the width and
        // height of an image that we can create that is it the product
        // width*height cannot be bigger than the maximum integer.
        //
        // Well a way to pass beyond that is to use TileWidth and TileHeight
        // instead of the real width and height when creating the sample
        // model. It will work!
        //
        // //
        if (tileSize < 0)
            setSampleModel(new BandedSampleModel(buffer_type, tileWidth,
                    tileHeight, tileWidth, banks, offsetsR));
        else
            setSampleModel(new PixelInterleavedSampleModel(buffer_type,
                    tileWidth, tileHeight, numBands, tileWidth * numBands,
                    bandsOffset));

        // //
        //
        // Setting the Color Model
        //
        // //
        if (colorInterpretations[0] == gdalconstConstants.GCI_PaletteIndex) {
            IndexColorModel icm = pBand.GetRasterColorTable()
                    .getIndexColorModel(gdal.GetDataTypeSize(buf_type));

            // TODO: fix the SWIG wrapper to avoid alpha setting when undefined
            setColorModel(icm);
        } else
            setColorModel(GDALUtilities.buildColorModel(getSampleModel()));

        if (getColorModel() == null || getSampleModel() == null)
            return false;
        return true;
    }

    /**
     * Returns <code>true</code> since this object does not support the
     * <code>mergeTree</code>, <code>setFromTree</code>, and
     * <code>reset</code> methods.
     * 
     * @return <code>true</code> since this <code>IIOMetadata</code> object
     *         cannot be modified.
     */
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Method unsupported. Calling this method will throws an
     * <code>UnsupportedOperationException</code>
     * 
     * @see javax.imageio.metadata.IIOMetadata#mergeTree()
     * 
     * @see #isReadOnly()
     */
    public void mergeTree(String formatName, Node root)
            throws IIOInvalidTreeException {
        throw new UnsupportedOperationException(
                "mergeTree operation is not allowed");
    }

    /**
     * Method unsupported. Calling this method will throws an
     * <code>UnsupportedOperationException</code>
     * 
     * @see javax.imageio.metadata.IIOMetadata#reset()
     * 
     * @see #isReadOnly()
     */
    public void reset() {
        throw new UnsupportedOperationException(
                "reset operation is not allowed");
    }

    /** Returns the Ground Control Points */
    public synchronized List getGcps() {
        // TODO: actually the Java bindings do not work properly when getting
        // GCPs (the JVM crash). Uncomment the following code when the method
        // getting GCPs works fine
        //
        //
//        if (super.getGcps().isEmpty()) {
//            final int nGCP = getGcpNumber();
//            List gcps = new Vector(nGCP);
//            final Dataset ds = GDALUtilities.acquireDataSet(getDatasetName(),
//                    gdalconst.GA_ReadOnly);
//            ds.GetGCPs((Vector) gcps);
//              if (gcps!=null && gcps.isEmpty()){
//                  List groundControlPoints = new ArrayList(nGCP);
//                  Iterator it = gcps.iterator();
//                  while (it.hasNext()){
//                      groundControlPoints.add(new GDALGCP((org.gdal.gdal.GCP)it.next()));
//                  }
//                  setGcps(groundControlPoints);
//              }
//            GDALUtilities.closeDataSet(ds);
//        }
        return super.getGcps();
    }

    // ////////////////////////////////////////////////////////////////////////
    //
    // Bands Properties
    // 
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Provides to increase data access performances. In many cases, the block
     * size for a raster band is a single line of N pixels, where N is the width
     * of the raster.
     * 
     * The Java Advanced Imaging allows to load and manipulate data only when
     * they are needed (The Deferred Execution Model). This is done by working
     * only on tiles containing the required data.
     * 
     * However, reading a big image composed of tiles having a size of Nx1 (The
     * most commonly used block size) would be not optimized because for each
     * tile, a read operation is computed (very small tiles -> very high read
     * operations number)
     * 
     * In order to optimize data access operations, it would be better to make
     * tiles a little bit greater than just a single line of pixels.
     * 
     * @param dataset
     */
    private void performTileSizeTuning(Dataset dataset) {
        final int width = dataset.getRasterXSize();
        final int height = dataset.getRasterYSize();
        final Dimension imageSize = new Dimension(width, height);
        final Dimension tileSize = GDALUtilities.toTileSize(imageSize);
        setTileHeight(tileSize.height);
        setTileWidth(tileSize.width);
    }

    /**
     * Returns a Map representing metadata elements (key,value) for a specific
     * domain of GDAL metadata.
     * 
     * @param metadataDomain
     *                the requested GDAL metadata domain.
     * 
     * @see GDALUtilities.GDALMetadataDomain
     * @return the metadata mapping for the specified domain or
     *         <code>null</code> in case no metadata is available for the
     *         domain or the specified domain is unsupported.
     */
    protected Map getGdalMetadataDomain(final String metadataDomain) {
        if (metadataDomain
                .equalsIgnoreCase(GDALUtilities.GDALMetadataDomain.DEFAULT)) {
            if (gdalMetadataMap
                    .containsKey(GDALUtilities.GDALMetadataDomain.DEFAULT_KEY_MAP))
                return (Map) gdalMetadataMap
                        .get(GDALUtilities.GDALMetadataDomain.DEFAULT_KEY_MAP);
        } else if (metadataDomain
                .equalsIgnoreCase(GDALUtilities.GDALMetadataDomain.IMAGESTRUCTURE)
                || metadataDomain
                        .startsWith(GDALUtilities.GDALMetadataDomain.XML_PREFIX)) {
            if (gdalMetadataMap.containsKey(metadataDomain))
                return (Map) gdalMetadataMap.get(metadataDomain);
        }
        return null;
    }

    /**
     * Return all the available metadata domains.
     * 
     * @return a list of <code>String</code>s representing metadata domains
     *         defined for the dataset on which this instance is based.
     */
    protected List getGdalMetadataDomainsList() {
        final Set keys = gdalMetadataMap.keySet();
        List list = null;
        // //
        // 
        // Since the GDAL default metadata domain is an empty String (which
        // can't be used as a key of a map), I need a minor tuning leveraging
        // on a valid String (see
        // GDALUtilities.GDALMetadataDomain.DEFAULT_KEY_MAP)
        //
        // //
        if (keys != null) {
            final Iterator keysIt = keys.iterator();
            list = new ArrayList(keys.size());
            while (keysIt.hasNext()) {
                final String key = (String) keysIt.next();
                if (key
                        .equals(GDALUtilities.GDALMetadataDomain.DEFAULT_KEY_MAP))
                    list.add(GDALUtilities.GDALMetadataDomain.DEFAULT);
                else
                    list.add(key);
            }
        }
        return list;
    }

    /**
     * Returns a copy of this <code>GDALCommonIIOImageMetadata</code> as a
     * <code>GDALWritableCommonIIOImageMetadata</code> instance, with setting
     * capabilities
     */
    /**
     * Returns a copy of this <code>GDALCommonIIOImageMetadata</code> as a
     * <code>GDALWritableCommonIIOImageMetadata</code> instance, with setting
     * capabilities
     */
    public GDALWritableCommonIIOImageMetadata asWritable() {
        GDALWritableCommonIIOImageMetadata metadata = new GDALWritableCommonIIOImageMetadata(
                this.getDatasetName());
        metadata.setDatasetDescription(this.getDatasetDescription());
        metadata.setProjection(this.getProjection());
        metadata.setGcpNumber(this.getGcpNumber());
        metadata.setGcpProjection(this.getGcpProjection());
        metadata.setGeoTransformation(this.getGeoTransformation());
        if (this.gdalMetadataMap != null) {
            Map inputMap = this.gdalMetadataMap;
            Map map = Collections.synchronizedMap(new HashMap(inputMap.size()));
            final Iterator outKeys = inputMap.keySet().iterator();
            while (outKeys.hasNext()) {
                final String key = (String) outKeys.next();
                final Map valuesMap = (Map) inputMap.get(key);
                final Iterator inKeys = valuesMap.keySet().iterator();
                final Map innerMap = new HashMap(valuesMap.size());
                while (inKeys.hasNext()) {
                    final String ikey = (String) inKeys.next();
                    final String value = (String) valuesMap.get(ikey);
                    innerMap.put(ikey, value);
                }
                map.put(key, innerMap);
            }
            metadata.gdalMetadataMap = map;
        }
        // TODO: Need to clone GCPs ... but actually JVM crashes when getting
        // GCPs
        metadata.setWidth(this.getWidth());
        metadata.setHeight(this.getHeight());
        metadata.setTileHeight(this.getTileHeight());
        metadata.setTileWidth(this.getTileWidth());

        metadata.setSampleModel(null);
        SampleModel sm = this.getSampleModel();
        if (sm != null) {
            final int smWidth = sm.getWidth();
            final int smHeight = sm.getHeight();
            metadata.setSampleModel(sm.createCompatibleSampleModel(smWidth,
                    smHeight));
        }
        metadata.setNumBands(this.getNumBands());

        metadata.setColorModel(null);
        ColorModel cm = this.getColorModel();
        if (cm != null) {
            if (cm instanceof IndexColorModel) {
                // //
                // TODO: Check this approach
                // //
                IndexColorModel icm = (IndexColorModel) cm;
                final int mapSize = icm.getMapSize();
                byte[] r = new byte[mapSize];
                byte[] g = new byte[mapSize];
                byte[] b = new byte[mapSize];

                icm.getBlues(b);
                icm.getReds(r);
                icm.getGreens(g);

                if (icm.hasAlpha()) {
                    byte[] a = new byte[mapSize];
                    icm.getAlphas(a);
                    metadata.setColorModel(new IndexColorModel(icm
                            .getPixelSize(), mapSize, r, g, b, a));
                } else
                    metadata.setColorModel(new IndexColorModel(icm
                            .getPixelSize(), mapSize, r, g, b));
            } else
                metadata.setColorModel(GDALUtilities.buildColorModel(metadata
                        .getSampleModel()));
        }

        metadata.setMaximums(this.getMaximums());
        metadata.setMinimums(this.getMinimums());
        metadata.setNoDataValues(this.getNoDataValues());
        metadata.setScales(this.getScales());
        metadata.setOffsets(this.getOffsets());

        metadata.setNumOverviews(this.getNumOverviews());
        metadata.setColorInterpretations(this.getColorInterpretations());
        return metadata;
    }
}
