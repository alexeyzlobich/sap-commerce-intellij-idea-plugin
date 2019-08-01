/*
 * This file is part of "SAP Integration" plugin for Intellij IDEA.
 * Copyright (C) 2014-2016 Alexander Bartash <AlexanderBartash@gmail.com>
 * Copyright (C) 2019 EPAM Systems <hybrisideaplugin@epam.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.intellij.idea.plugin.hybris.tools.remote.http.solr

import com.intellij.idea.plugin.hybris.settings.HybrisDeveloperSpecificProjectSettingsComponent
import com.intellij.idea.plugin.hybris.settings.HybrisRemoteConnectionSettings
import com.intellij.idea.plugin.hybris.tools.remote.http.SolrHttpClient
import com.intellij.idea.plugin.hybris.tools.remote.http.impex.HybrisHttpResult
import com.intellij.openapi.project.Project
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.impl.NoOpResponseParser
import org.apache.solr.client.solrj.request.CoreAdminRequest
import org.apache.solr.client.solrj.request.QueryRequest
import org.apache.solr.client.solrj.response.CoreAdminResponse
import org.apache.solr.common.params.CoreAdminParams
import org.apache.solr.common.params.SolrParams
import java.io.IOException
import java.util.ArrayList
import kotlin.jvm.internal.Intrinsics

object SolrHttpClientImpl : SolrHttpClient {

    override fun executeSolrQuery(project: Project, queryObject: SolrQueryObject): HybrisHttpResult {
        val remoteConnectionSettings = solrConnectionSettings(project)
        val httpSolrClient = HttpSolrClient.Builder(remoteConnectionSettings.generatedURL + '/'.toString() + queryObject.core).build()

        var hybrisHttpResult: HybrisHttpResult
        try {
            val solrQuery = SolrQuery(queryObject.query)
            solrQuery.rows = queryObject.rows
            solrQuery.set("indent", "on")
            solrQuery.set("debug", "query")
            val queryRequest = QueryRequest(solrQuery as SolrParams)
            queryRequest.setBasicAuthCredentials(remoteConnectionSettings.adminLogin, remoteConnectionSettings.adminPassword)
            val noOpResponseParser = NoOpResponseParser()
            noOpResponseParser.writerType = "json"
            queryRequest.responseParser = noOpResponseParser
            val response = httpSolrClient.request(queryRequest as SolrRequest<*>).get("response")
            if (response == null) {
                throw TypeCastException("null cannot be cast to non-null type kotlin.String")
            } else {
                val responseString = response.toString()
                hybrisHttpResult = HybrisHttpResult.HybrisHttpResultBuilder.createResult().output(responseString).build()
                Intrinsics.checkExpressionValueIsNotNull(hybrisHttpResult, "hybrisHttpResult")
                return hybrisHttpResult
            }
        } catch (e: Exception) {
            hybrisHttpResult = HybrisHttpResult.HybrisHttpResultBuilder.createResult().errorMessage(e.message).httpCode(500).build()
            Intrinsics.checkExpressionValueIsNotNull(hybrisHttpResult, "hybrisHttpResult")
            return hybrisHttpResult
        }

    }

    override fun listOfCores(project: Project): Array<String> {
        val remoteConnectionSettings = solrConnectionSettings(project)

        val listOfCores: Array<String>
        listOfCores = try {
            this.listOfCores(project, remoteConnectionSettings)
        } catch (e: Exception) {
            Array(0) { null.toString() }
        }
        return listOfCores
    }

    override fun listOfCores(project: Project, solrConnectionSettings: HybrisRemoteConnectionSettings): Array<String> {
        val baseSolrUrl = solrConnectionSettings.generatedURL
        val httpSolrClient = HttpSolrClient.Builder(baseSolrUrl).build()
        val coreAdminRequest = CoreAdminRequest()
        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS)
        coreAdminRequest.setBasicAuthCredentials(solrConnectionSettings.adminLogin, solrConnectionSettings.adminPassword)
        var coreAdminResponse: CoreAdminResponse? = null
        try {
            coreAdminResponse = coreAdminRequest.process(httpSolrClient as SolrClient) as CoreAdminResponse
        } catch (e: SolrServerException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val listOfCores = ArrayList<String>()
        var counterOfCores = 0
        Intrinsics.checkExpressionValueIsNotNull(coreAdminResponse!!, "cores")

        val coreAdminResponseStatusSize = coreAdminResponse.coreStatus.size()
        while (counterOfCores < coreAdminResponseStatusSize) {
            listOfCores.add(coreAdminResponse.coreStatus.getName(counterOfCores))
            ++counterOfCores
        }
        return listOfCores.toTypedArray()
    }

    private fun solrConnectionSettings(project: Project): HybrisRemoteConnectionSettings {
        val remoteConnectionSettings = HybrisDeveloperSpecificProjectSettingsComponent.getInstance(project).getActiveSolrConnectionSettings(project)
        Intrinsics.checkExpressionValueIsNotNull(remoteConnectionSettings, "HybrisDeveloperSpecificP…nnectionSettings(project)")
        return remoteConnectionSettings
    }
}