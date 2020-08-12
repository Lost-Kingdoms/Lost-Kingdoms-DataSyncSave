
public class MissingOrganizedObjectKeyException extends Exception{

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -329124217073195644L;

	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public MissingOrganizedObjectKeyException(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "MissingOrganizedObjectKeyException: " + "Every OrganizedDataObject needs a key in its annotation!";
	}
	
}
