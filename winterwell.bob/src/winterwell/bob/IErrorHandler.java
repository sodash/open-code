package winterwell.bob;

public interface IErrorHandler {

	void handle(Throwable ex) throws Exception;
	
}
