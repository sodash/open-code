package com.winterwell.maths;

import java.util.List;

/**
 * Interface for things which can be trained. This interface supports batch and
 * online training, supervised and unsupervised. Implementations may not support
 * all the methods.
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Create
 * <li>call {@link #resetup()}
 * <li>train
 * <li>call {#isReady()} and {@link #finishTraining()} if not
 * <li>use the classify methods
 * <li>potentially call {@link #resetup()} to reset and retrain (e.g. in k-fold
 * testing)
 * </ol>
 * 
 * @author daniel
 * 
 */
public interface ITrainable<DataType> {

	/**
	 * This is pretty much identical to {@link Supervised}, except with the
	 * types sort of reversed.
	 * 
	 * @param <Context>
	 * @param <X>
	 */
	public static interface CondUnsupervised<Context, X> extends
			ITrainable<Context> {

		/**
		 * UnSupervised training for conditional distributions.
		 */
		void train1(Context context, X x, double weight);

	}


	public static interface Seqn<Observed> extends ITrainable<Observed> {
		/**
		 * Supervised training for sequence distributions, e.g. HMMs.
		 */
		void trainSeqn(List<Observed> observed);
	}

	public static interface Seqn2Layer<Observed, Hidden> extends
			ITrainable<Observed> {
		/**
		 * Supervised training for sequence distributions, e.g. HMMs.
		 */
		void trainSeqn(List<Observed> observed, List<Hidden> hidden);
	}

	/**
	 * @author daniel
	 * 
	 * @param <DataType>
	 *            the input type -- for an ICondDistribution this is Cntxt of
	 *            similar
	 * @param <Tag>
	 *            the output label -- for an ICondDistribution this is the
	 *            Sitn.outcome
	 */
	public static interface Supervised<DataType, Tag> extends
			ITrainable<DataType> {

		/**
		 * Supervised training.
		 * 
		 * @param x
		 *            example datum to learn from
		 * @param tag
		 *            never null
		 * @param weight Note: If the model does not support weights, it may ignore this.           
		 */
		void train1(DataType x, Tag tag, double weight);
		

		/**
		 * Supervised training, weight=1
		 * 
		 * @param x
		 *            example datum to learn from
		 * @param tag
		 *            "y" the target variable, never null
		 * @see #train1(Object, Object, double)            
		 */
		default void train1(DataType x, Tag tag) {
			train1(x, tag, 1);
		}

	}

	public static interface Unsupervised<DataType> extends ITrainable<DataType> {
		/**
		 * Unsupervised training with larger batches.
		 * 
		 * @param data
		 *            example data to learn from
		 */
		default void train(Iterable<? extends DataType> data) {
			for (DataType x : data) {
				train1(x);
			}
		}

		/**
		 * Unsupervised training
		 * 
		 * @param x
		 *            example datum to learn from. Must not be null.
		 */
		// Note: this has a different name from train to avoid confusion
		// over which method gets called if DataType is Iterable
		void train1(DataType data) throws UnsupportedOperationException;

		/**
		 * It was discussed whether to call this "bodybuilder", or "gymrat".
		 * @author daniel
		 *
		 * @param <DataType>
		 */
		public static interface Weighted<DataType> extends Unsupervised<DataType> {
			/**
			 * Unsupervised training with larger batches.
			 * 
			 * @param x
			 *            example dats to learn from
			 */
			default void train(double[] weights, Iterable<? extends DataType> data) {
				int i=0;
				for(DataType x : data) {
					double w = weights[i];
					i++;
					train1(x, w);
				}
			}
			
			/**
			 * default to weight=1 and call {@link #train1(Object, double)}
			 */
			@Override
			default void train1(DataType data) throws UnsupportedOperationException {
				train1(data, 1);
			}

			/**
			 * Unsupervised training
			 * 
			 * @param x
			 *            example datum to learn from. Must not be null.
			 */
			void train1(DataType data, double weight) throws UnsupportedOperationException;

		}
		
	}

	/**
	 * If this is a batch-trained system, prepare it for use. Does nothing for
	 * online-trained systems. Typically this method can clear any caches e.g.
	 * of training data that were created in the course of training.
	 */
	void finishTraining();

	/**
	 * Is the {@link ITrainable} ready to be trained?
	 * 
	 * @return
	 */
	boolean isReady();

	/**
	 * If not trained, get ready for training. If trained, discard all learned
	 * knowledge (but *not* any configuration settings). I.e. this is both init
	 * and reset.
	 */
	void resetup();

}
