package org.rooftop.pay.infra.transaction

import com.github.f4b6a3.tsid.TsidFactory
import org.rooftop.pay.app.TransactionIdGenerator
import org.springframework.stereotype.Component

@Component
class TsidTransactionIdGenerator : TransactionIdGenerator {

    override fun generate(): String = tsidFactory.create().toLong().toString()

    private companion object {
        private val tsidFactory = TsidFactory.newInstance256(110)
    }
}
