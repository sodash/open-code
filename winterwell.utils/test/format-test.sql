
		SELECT
			txnDataUnMatched.[LedgerID],
			txnDataUnMatched.[txnUID],
			txnDataUnMatched.[txndate],
			
			txnDataUnMatched.[NetAmount],				
			1 AS [Count]								
					
		INTO
			#LeftSource
		FROM
			[dbo].txnDataUnMatched
		WHERE
			RecID = @RecID
		AND	SourceID = @SourceA
		AND  [TxnDataUnmatched].[Ref11] LIKE ''British Car Au%''
								
		
		SELECT @retVal = @@ERROR, @numMatches = @@ROWCOUNT
		
		SELECT
			IDENTITY(int, 1,1) [MatchID],
			#LeftSource.[LedgerID], 
			#LeftSource.[txnUID] [LeftID],
			#LeftSource.[Count] [LeftCount],
			#LeftSource.[NetAmount] [LeftNetAmount],
			#RightSource.[txnUID] [RightID],
			#RightSource.[Count] [RightCount],
			#RightSource.[NetAmount] [RightNetAmount],
			-(#LeftSource.Netamount + (#RightSource.Netamount * @MatchType)) [AdjustmentAmount],
			#RightSource.[txnUID] [OriginalTxnUID]
		INTO
			#Match
		FROM
			#LeftSource
			INNER JOIN #RightSource
				ON #LeftSource.LedgerID = #RightSource.LedgerID
				
				AND ABS(#LeftSource.NetAmount + (#RightSource.NetAmount * @MatchType))=0
					
			AND ( [#LEFTSOURCE].[txndate]<=[#RIGHTSOURCE].[txndate])		

		SELECT @retVal = @@ERROR, @numMatches = @@ROWCOUNT
		﻿
