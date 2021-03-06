package systems.dmx.images;


import java.awt.image.BufferedImage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;
import org.imgscalr.Scalr;
import systems.dmx.core.Assoc;
import systems.dmx.core.Constants;
import systems.dmx.core.Topic;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.files.FilesService;
import systems.dmx.files.ItemKind;
import systems.dmx.files.ResourceInfo;
import systems.dmx.files.StoredFile;
import systems.dmx.files.UploadedFile;

/**
 * DMX Plugin to handle image uploads and resize images.
 */
@Path("/images")
public class ImagePlugin extends PluginActivator implements ImageService {
    
    private static Logger log = Logger.getLogger(ImagePlugin.class.getName());

    public static final String FILEREPO_BASE_URI_NAME       = "filerepo";
    public static final String FILEREPO_IMAGES_SUBFOLDER    = "images";
    public static final String DM4_HOST_URL = System.getProperty("dmx.host.url");

    public static final String RESIZED_IMAGE = "dmx.images.resized_image";

    @Inject private FilesService files;

    /**
     * Standard image upload integration.
     * @param image     Uploaded file resource.
     * @return topic    File Topic
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Topic upload(UploadedFile image) {
        String imagesFolderPath = getImagesDirectoryInFileRepo();
        log.info("Upload image " + image.getName() + " to filerepo folder=" + imagesFolderPath);
        try {
            StoredFile file = files.storeFile(image, imagesFolderPath);
            String path = imagesFolderPath + "/" + file.getFileName();
            return files.getFileTopic(path);
        } catch (Exception e) {
            throw new RuntimeException("Uploading IMAGE file \"" + image.getName() + "\" FAILED", e);
        }
    }

    @POST
    @Path("/resize/{topicId}/{maxSize}/{mode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public RelatedTopicModel resizeImageFileTopic(@PathParam("topicId") long fileTopicId, @PathParam("maxSize") int maxSize,
            @PathParam("mode") String mode) {
        log.info("File Topic to Resize (ID: " + fileTopicId + ") Max Size: " + maxSize + "px");
        try {
            File fileTopicFile = files.getFile(fileTopicId);
            String fileTopicFileName = fileTopicFile.getName();
            Topic fileTopic = dmx.getTopic(fileTopicId).loadChildTopics();
            String fileTopicRepositoryPath = fileTopic.getChildTopics().getString("dmx.files.path");
            String fileMediaType = fileTopic.getChildTopics().getString("dmx.files.media_type");
            if (fileMediaType.contains("jpeg") || fileMediaType.contains("jpg") || fileMediaType.contains("png")) {
                log.info("Image File Topic Path requested to be RESIZED: " + fileTopicRepositoryPath);
                BufferedImage srcImage = ImageIO.read(fileTopicFile); // Load image
                log.info("Image File Buffered " + srcImage); // Print Image Metadata
                BufferedImage scaledImage = null;
                if (mode.equals("width")) {
                    scaledImage = Scalr.resize(srcImage, org.imgscalr.Scalr.Mode.FIT_TO_WIDTH, maxSize);
                } else if (mode.equals("height")) {
                    scaledImage = Scalr.resize(srcImage, org.imgscalr.Scalr.Mode.FIT_TO_HEIGHT, maxSize);
                } else {
                    scaledImage = Scalr.resize(srcImage, org.imgscalr.Scalr.Mode.AUTOMATIC, maxSize);
                }
                String imageFileEnding = fileTopicFileName.substring(fileTopicFileName.indexOf(".") + 1);
                String imageFileTopicParentRepositoryPath = getParentFolderRepositoryPath(fileTopicRepositoryPath);
                String newFileName = calculateResizedFilename(fileTopicFileName, maxSize + "");
                File resizedImageFile = new File(fileTopicFile.getParent() + "/" + newFileName);
                if (resizedImageFile.createNewFile()) {
                    ImageIO.write(scaledImage, imageFileEnding, resizedImageFile);
                    log.info("Resized Image File \"" + resizedImageFile.getAbsolutePath() + "\" CREATED");
                } else {
                    // Fixme: Why is that? Does imgscalr only handle "jpeg"?
                    if (imageFileEnding.equals("jpg")) imageFileEnding = "jpeg";
                    ImageIO.write(scaledImage, imageFileEnding, resizedImageFile);
                    log.warning("Image File already exists \"" + resizedImageFile.getPath() + "\" - REWRITE");
                }
                // Create File topic for newly created file
                Topic resizedImageFileTopic = files.getFileTopic(
                    imageFileTopicParentRepositoryPath + "/" + newFileName);
                // Associate new file topic with original file topic
                return createResizedImageAssociation(fileTopic, resizedImageFileTopic);
            } else {
                throw new WebApplicationException(new RuntimeException("Sorry! At the moment we can "
                    + "only resize JPGs or PNGs and not file topics with MediaType: " + fileMediaType),
                    Response.Status.BAD_REQUEST);
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    private String calculateResizedFilename(String originalFilename, String sizeParameter) {
        String imageFileEnding = originalFilename.substring(originalFilename.indexOf(".") + 1);
        String imageFileBeginning = originalFilename.substring(originalFilename.lastIndexOf("/") + 1, originalFilename.indexOf("."));
        return imageFileBeginning + "-" + sizeParameter + "px." + imageFileEnding;
    }

    private String getParentFolderRepositoryPath(String fileTopicRepositoryPath) {
        String pathSeperator = "/";
        /** if (System.getProperty("os.name").contains("Win")) {
            log.info("Lookup parent folder on " + System.getProperty("os.name") + " escaping the File seperator \\");
            pathSeperator = "\\";
        } **/
        return fileTopicRepositoryPath.substring(0, fileTopicRepositoryPath.lastIndexOf(pathSeperator));
    }

    private RelatedTopicModel createResizedImageAssociation(Topic original, Topic resized) {
        Assoc exists = original.getAssoc(RESIZED_IMAGE, null, null, resized.getId());
        if (exists == null) {
            exists = dmx.createAssoc(mf.newAssocModel(RESIZED_IMAGE,
                mf.newTopicPlayerModel(original.getId(), Constants.PARENT),
                mf.newTopicPlayerModel(resized.getId(), Constants.CHILD)));
        } else {
            log.info("Resize Image Notice: Rewrote the image files contents but did not create a new file topic");
        }
        return mf.newRelatedTopicModel(
            resized.getModel(), exists.getModel()
        );
    }

    /**
     * Constructs path to the "images" folder, either in a workspace or in a global file repository.
     * @return String   Repository path for storing images.
     */
    private String getImagesDirectoryInFileRepo() {
        String folderPath = FILEREPO_IMAGES_SUBFOLDER; // global filerepo
        if (!files.pathPrefix().equals("/")) { // add workspace specific path in front of image folder name
            folderPath = files.pathPrefix() + "/" + FILEREPO_IMAGES_SUBFOLDER;
        }
        try {
            // check image file repository
            if (files.fileExists(folderPath)) {
                ResourceInfo resourceInfo = files.getResourceInfo(files.pathPrefix() + "/" + FILEREPO_IMAGES_SUBFOLDER);
                if (resourceInfo.getItemKind() != ItemKind.DIRECTORY) {
                    String message = "ImagePlugin: \"images\" storage directory in repo path " + folderPath + " can not be used";
                    throw new IllegalStateException(message);
                }
            } else { // images subdirectory does not exist yet in repo
                log.info("Creating the \"images\" subfolder on the fly for new filerepo in " + folderPath+ "!");
                // A (potential) workspace folder gets created on the fly, too (since #965).
                files.createFolder(FILEREPO_IMAGES_SUBFOLDER, files.pathPrefix());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return folderPath;
    }

}
