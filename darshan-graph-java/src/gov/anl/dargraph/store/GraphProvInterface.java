package gov.anl.dargraph.store;

public interface GraphProvInterface {

	void userStartJob(String user_id, String job_id, long start_time,
			long end_time);

	void jobFromObj(String job_id, String obj_id, long start_time, long end_time);

	void jobHasProcs(String job_id, String proc_id);

	void procReadsFile(String proc_id, String file_id, long reads);

	void shutdown();

	void procWritesFile(String proc_id, String file_id, long writes);
}
