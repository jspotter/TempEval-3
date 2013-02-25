$ cd tools 

> python TE3-evaluation.py gold_folder_or_file system_folder_or_filefile
$ python TE3-evaluation.py data/gold data/system 
# runs with debug level 0 and only reports the performance; also creates a temporary folder to create normalized files  

> python TE3-evaluation.py gold_folder_or_file system_folder_or_filefile debug_level 
$ python TE3-evaluation.py data/gold data/system 0.5
# with debug level 0.5, print DETAILED entity feature performance, e.g. class accuracy, precision, recall, etc.  
$ python TE3-evaluation.py data/gold data/system 1  
# based on the debug_level print debug information. also try debug level 2. 

> python TE3-evaluation.py gold_folder_or_file system_folder_or_filefile debug_level tmp_folder
$ python TE3-evaluation.py data/gold data/system 1 tmp-to-be-deleted
# additionally creates the temporary folder to put normalized files


> python TE3-evaluation.py gold_folder_or_file system_folder_or_filefile debug_level tmp_folder evaluation_method 
$ python TE3-evaluation.py data/gold data/system 0 tmp-to-be-deleted acl11
$ python TE3-evaluation.py data/gold data/system 0 tmp-to-be-deleted implicit_in_recall
# run with different evaluation methods. acl11 to run with ACL'11 short paper metric, not considering the reduced graph for relations. implicit_in_recall to reward the implicit relation in recall as well. 

## usage: ## to check the performance of a single file: ##          python TE3-evaluation.py gold_file_path system_file_path debug_level## to check the performace of all files in a gold folder: ##          python TE3-evaluation.py gold_folder_path system_folder_path debug_level
