package com.expensetracker.presentation.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class ChatMessage(
    val role: String,           // "user" | "assistant"
    val text: String,
    val isLoading: Boolean = false
)

data class FinancialChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            role = "assistant",
            text = "Hi! I'm your financial assistant. Ask me anything about your transactions, " +
                    "spending patterns, account balances, or budgets.\n\n" +
                    "**Examples:**\n" +
                    "• Total spent on Food & Dining this month\n" +
                    "• My top 5 expenses last year\n" +
                    "• How much did I earn vs spend in 2025?\n" +
                    "• Monthly spending trend for last 6 months"
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FinancialChatViewModel @Inject constructor(
    private val queryEngine: FinancialQueryEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialChatUiState())
    val uiState: StateFlow<FinancialChatUiState> = _uiState.asStateFlow()

    // Conversation history sent to the API (excludes the welcome message)
    private val apiHistory = mutableListOf<JSONObject>()

    fun setInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        // Add user message to UI
        _uiState.update { state ->
            state.copy(
                messages   = state.messages + ChatMessage("user", text),
                inputText  = "",
                isLoading  = true,
                error      = null
            )
        }

        // Add loading indicator
        _uiState.update { state ->
            state.copy(messages = state.messages +
                    ChatMessage("assistant", "", isLoading = true))
        }

        // Add to API history
        apiHistory += JSONObject().apply {
            put("role", "user")
            put("content", text)
        }

        viewModelScope.launch {
            try {
//                val reply = callClaudeWithTools(apiHistory)
                val reply = callGeminiWithTools(apiHistory)
                // Replace loading indicator with real response
                _uiState.update { state ->
                    state.copy(
                        messages  = state.messages.dropLast(1) +
                                ChatMessage("assistant", reply),
                        isLoading = false
                    )
                }
                apiHistory += JSONObject().apply {
                    put("role", "assistant")
                    put("content", reply)
                }
            } catch (e: Exception) {
                Log.e("Expense Tracker", "exception = ${e.message}")
                _uiState.update { state ->
                    state.copy(
                        messages  = state.messages.dropLast(1),
                        isLoading = false,
                        error     = "Error: ${e.message ?: "Something went wrong"}"
                    )
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun clearChat() {
        apiHistory.clear()
        _uiState.update { FinancialChatUiState() }
    }

    // ── Claude API call with tool-use loop ────────────────────────────────────

    private suspend fun callClaudeWithTools(history: List<JSONObject>): String {
        // Build tools definition
        val tools = buildToolDefinitions()

        // System prompt
        val systemPrompt = """
You are a helpful personal finance assistant embedded in an expense tracking app.
You have access to the user's real financial data via tools.

Guidelines:
- Always use tools to fetch actual data before answering quantitative questions.
- Present numbers clearly with currency symbol (₹) and proper formatting.
- When summarising, use clear tables or bullet points.
- If a category/mode name is ambiguous, use the closest match.
- For date filters: use today's date as reference when user says "this month", "this year", etc.
- Today's date: ${java.time.LocalDate.now()}.
- Be concise but complete. Don't ask clarifying questions unless truly necessary.
        """.trimIndent()

        // Agentic loop: keep calling until no more tool calls
        val workingHistory = history.map { it }.toMutableList()
        var iterations = 0

        while (iterations++ < 5) {
            val requestBody = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 2048)
                put("system", systemPrompt)
                put("messages", JSONArray(workingHistory))
                put("tools", tools)
            }

            val response = makeApiCall(requestBody)
            val stopReason = response.optString("stop_reason")
            val contentArr = response.getJSONArray("content")

            // Collect text blocks and tool use blocks
            val textBlocks  = mutableListOf<String>()
            val toolUseBlocks = mutableListOf<JSONObject>()

            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                when (block.optString("type")) {
                    "text"     -> textBlocks += block.getString("text")
                    "tool_use" -> toolUseBlocks += block
                }
            }

            if (stopReason == "end_turn" || toolUseBlocks.isEmpty()) {
                return textBlocks.joinToString("\n").trim()
            }

            // Add assistant turn (with tool_use blocks) to history
            workingHistory += JSONObject().apply {
                put("role", "assistant")
                put("content", contentArr)
            }

            // Execute each tool and build tool_result blocks
            val resultBlocks = JSONArray()
            for (toolBlock in toolUseBlocks) {
                val toolName  = toolBlock.getString("name")
                val toolId    = toolBlock.getString("id")
                val toolInput = toolBlock.optJSONObject("input") ?: JSONObject()
                val result    = try {
                    queryEngine.execute(toolName, toolInput)
                } catch (e: Exception) {
                    """{"error":"${e.message}"}"""
                }
                resultBlocks.put(JSONObject().apply {
                    put("type",        "tool_result")
                    put("tool_use_id", toolId)
                    put("content",     result)
                })
            }

            // Add tool results to history
            workingHistory += JSONObject().apply {
                put("role", "user")
                put("content", resultBlocks)
            }
        }

        return "I wasn't able to complete the analysis. Please try rephrasing your question."
    }

    private suspend fun callGeminiWithTools(history: List<JSONObject>): String {

        val tools = buildGeminiToolDefinitions()

        val contents = JSONArray()

        // Convert history → Gemini format
        history.forEach {
            contents.put(
                JSONObject().apply {
                    put("role", it.getString("role"))
                    put("parts", JSONArray().put(
                        JSONObject().put("text", it.getString("content"))
                    ))
                }
            )
        }

        var iterations = 0

        while (iterations++ < 5) {

            val requestBody = JSONObject().apply {
                put("contents", contents)
                put("tools", tools)
            }

            val response = makeGeminiApiCall(requestBody)

            val candidates = response.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")

            val functionCalls = mutableListOf<JSONObject>()
            val textResponses = mutableListOf<String>()

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)

                if (part.has("functionCall")) {
                    functionCalls += part.getJSONObject("functionCall")
                }

                if (part.has("text")) {
                    textResponses += part.getString("text")
                }
            }

            // ✅ If no tool call → final answer
            if (functionCalls.isEmpty()) {
                return textResponses.joinToString("\n").trim()
            }

            // Add model response to history
            contents.put(content)

            // Execute tools
            for (call in functionCalls) {
                val name = call.getString("name")
                val args = call.optJSONObject("args") ?: JSONObject()

                val result = try {
                    queryEngine.execute(name, args)
                } catch (e: Exception) {
                    """{"error":"${e.message}"}"""
                }

                // Send result back to Gemini
                contents.put(
                    JSONObject().apply {
                        put("role", "tool")
                        put("parts", JSONArray().put(
                            JSONObject().apply {
                                put("functionResponse", JSONObject().apply {
                                    put("name", name)
                                    put("response", JSONObject().put("result", result))
                                })
                            }
                        ))
                    }
                )
            }
        }

        return "Couldn't complete the request. Try rephrasing."
    }

    private suspend fun makeApiCall(body: JSONObject): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${getApiKey()}")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                Log.d("ExpenseTracker", "apiKey = ${getApiKey()}")
                conn.setRequestProperty("x-api-key", getApiKey())
                conn.setRequestProperty("anthropic-version", "2023-06-01")
                conn.doOutput = true
                conn.connectTimeout = 30_000
                conn.readTimeout    = 60_000

                conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

                val code = conn.responseCode
                val responseText = if (code == 200)
                    conn.inputStream.bufferedReader().readText()
                else
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"

                if (code != 200) {
                    throw Exception("API error $code: $responseText")
                }
                JSONObject(responseText)
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun makeGeminiApiCall(body: JSONObject): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${getApiKey()}"
            )
            val conn = url.openConnection() as HttpURLConnection

            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.bufferedWriter().use {
                    it.write(body.toString())
                }

                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response)

            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * API key storage.
     * In production, store this in BuildConfig (injected at build time) or
     * an encrypted DataStore. Never hardcode it.
     */
    private fun getApiKey(): String = "AIzaSyBWw8Rh39opzBUqR0_EHy6DgD1uORwt5y8"

    // ── Tool definitions sent to Claude ──────────────────────────────────────

    private fun buildToolDefinitions(): JSONArray {
        fun prop(type: String, desc: String) = JSONObject().apply {
            put("type", type); put("description", desc)
        }
        fun enumProp(desc: String, vararg values: String) = JSONObject().apply {
            put("type", "string"); put("description", desc)
            put("enum", JSONArray(values.toList()))
        }
        fun intProp(desc: String) = JSONObject().apply {
            put("type", "integer"); put("description", desc)
        }

        val sharedDateProps = JSONObject().apply {
            put("year",       intProp("Calendar year, e.g. 2025"))
            put("month",      intProp("Month number 1-12"))
            put("from_date",  prop("string", "Start date yyyy-MM-dd"))
            put("to_date",    prop("string", "End date yyyy-MM-dd"))
            put("category",   prop("string", "Category name (partial match)"))
            put("payment_mode", prop("string", "Payment mode name (partial match)"))
            put("tag",        prop("string", "Tag to filter by"))
        }

        fun tool(name: String, desc: String, props: JSONObject) = JSONObject().apply {
            put("name", name)
            put("description", desc)
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put("properties", props)
            })
        }

        return JSONArray(listOf(
            tool(
                "get_summary",
                "Get overall income, expense, transfer totals and net balance for a period.",
                sharedDateProps
            ),
            tool(
                "get_transactions",
                "List individual transactions matching filters. Returns up to 50 rows.",
                JSONObject(sharedDateProps.toString()).apply {
                    put("transaction_type", enumProp("Filter by type", "income","expense","transfer","all"))
                }
            ),
            tool(
                "summarise_by_category",
                "Group transactions by category and show totals. Great for 'how much did I spend on X'.",
                JSONObject(sharedDateProps.toString()).apply {
                    put("transaction_type", enumProp("Type to summarise", "income","expense","transfer","all"))
                }
            ),
            tool(
                "summarise_by_payment_mode",
                "Group transactions by payment mode / account and show totals.",
                JSONObject(sharedDateProps.toString()).apply {
                    put("transaction_type", enumProp("Type to summarise", "income","expense","transfer","all"))
                }
            ),
            tool(
                "get_account_balances",
                "Get current balances for all bank accounts and standalone payment modes.",
                JSONObject()
            ),
            tool(
                "get_top_expenses",
                "List the top N largest expense transactions for a period.",
                JSONObject(sharedDateProps.toString()).apply {
                    put("limit", intProp("Number of transactions to return (default 10)"))
                }
            ),
            tool(
                "get_spending_trend",
                "Get month-by-month expense totals for the last N months.",
                JSONObject().apply {
                    put("months", intProp("Number of recent months to analyse (default 6, max 24)"))
                }
            )
        ))
    }

    private fun buildGeminiToolDefinitions(): JSONArray {

        fun tool(name: String, desc: String, props: JSONObject): JSONObject {
            return JSONObject().apply {
                put("functionDeclarations", JSONArray().put(
                    JSONObject().apply {
                        put("name", name)
                        put("description", desc)
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", props)
                        })
                    }
                ))
            }
        }

        val shared = JSONObject().apply {
            put("category", JSONObject().put("type", "string"))
            put("payment_mode", JSONObject().put("type", "string"))
            put("year", JSONObject().put("type", "integer"))
            put("month", JSONObject().put("type", "integer"))
        }

        return JSONArray(listOf(
            tool("get_summary", "Get financial summary", shared),
            tool("get_transactions", "Get transactions", shared),
            tool("summarise_by_category", "Summarise by category", shared),
            tool("summarise_by_payment_mode", "Summarise by payment mode", shared),
            tool("get_account_balances", "Get balances", JSONObject()),
            tool("get_top_expenses", "Top expenses", shared),
            tool("get_spending_trend", "Spending trend", JSONObject())
        ))
    }
}