package systems.dmx.images;

import systems.dmx.core.Topic;
import systems.dmx.core.model.RelatedTopicModel;

/**
 *
 * @author Malte Reißig (<a href="mailto:malte@dmx.berlin>Email</a>)
 */
public interface ImageService {

    
    /**
     * Resizes an existing image represented by a DeepaMehta 4 file topic. File topic must already exist.
     * @param
     * fileTopicId  long    An ID of a "File" topic.
     * maxSize  int         The maximum size of the larger side (Mode: "Auto") or the "fit to width" or "fit to height" value in pixel.
     * fitTo    String      Either "width", or "height", otherwise "auto" is used.
     * @return  Topic   A new file topic representing the resized image.
     */
    RelatedTopicModel resizeImageFileTopic(long fileTopicId, int maxSize, String fitTo);

}
