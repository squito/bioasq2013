require('lda');

runlda <- function(data_directory, K = 20, num.iterations = 50, alpha = 0.1, eta = 0.1) {
	
	documents = read.documents(paste(data_directory,'/bioasqLDAc.dat', sep=''));	
	vocab = read.vocab(paste(data_directory,'/bioasqVocab.dat', sep=''));
	labels = read.vocab(paste(data_directory,'/bioasqLabelVocab.dat', sep=''));

	result = lda.collapsed.gibbs.sampler(documents, K, list(vocab, labels), num.iterations, alpha,
eta, initial = NULL, burnin = NULL, compute.log.likelihood = FALSE,
  trace = 1L, freeze.topics = FALSE)

	top.words = top.topic.words(result$topics, 5, by.score=TRUE)

	return(list(result = result, top.words = top.words))
}