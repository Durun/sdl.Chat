package jp.ac.titech.itpro.sdl.chat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import jp.ac.titech.itpro.sdl.chat.message.ChatMessage
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val chatLog = ArrayList<ChatMessage>()
    private lateinit var chatLogAdapter: ArrayAdapter<ChatMessage>
    private lateinit var adapter: BluetoothAdapter

    enum class State {
        Initializing, Disconnected, Connecting, Connected, Waiting
    }

    private var state = State.Initializing
    private var messageSeq = 0
    private var agent: Agent? = null
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var initializer: BluetoothInitializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        chatLogAdapter = object : ArrayAdapter<ChatMessage>(this, 0, chatLog) {
            override fun getView(pos: Int, view: View?, parent: ViewGroup): View {
                val v = view
                        ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
                val message = getItem(pos)!!
                val text1 = v.findViewById<TextView>(android.R.id.text1)
                message.sender?.let {
                    text1.setTextColor(if (it == adapter.name) Color.GRAY else Color.BLACK)
                }
                text1.text = message.content
                return v
            }
        }

        main_logview.adapter = chatLogAdapter
        val fmt = DateFormat.getDateTimeInstance()
        main_logview.onItemClickListener = OnItemClickListener { parent, _, pos, _ ->
            val msg = parent.getItemAtPosition(pos) as ChatMessage
            AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.msg_title, msg.seq, msg.sender))
                    .setMessage(getString(R.string.msg_content, msg.content, fmt.format(Date(msg.time))))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
        setState(State.Initializing)
        soundPlayer = SoundPlayer(this)
        initializer = object : BluetoothInitializer(this) {
            override fun onReady(adapter: BluetoothAdapter) {
                this@MainActivity.adapter = adapter
                setState(State.Disconnected)
            }
        }
        initializer.initialize()
    }

    private class CommHandler internal constructor(activity: MainActivity) : Handler() {
        val ref = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            Log.d(TAG, "handleMessage")
            val activity = ref.get() ?: return
            when (msg.what) {
                Agent.MSG_STARTED -> {
                    val device = msg.obj as BluetoothDevice
                    activity.setState(State.Connected, ScanActivity.caption(device))
                }
                Agent.MSG_FINISHED -> {
                    Toast.makeText(activity, R.string.toast_connection_closed, Toast.LENGTH_SHORT).show()
                    activity.setState(State.Disconnected)
                }
                Agent.MSG_RECEIVED -> activity.showMessage(msg.obj as ChatMessage)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        if (state == State.Connected) agent?.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onPrepareOptionsMenu")
        menu.findItem(R.id.menu_main_connect).isVisible = state == State.Disconnected
        menu.findItem(R.id.menu_main_disconnect).isVisible = state == State.Connected
        menu.findItem(R.id.menu_main_accept_connection).isVisible = state == State.Disconnected
        menu.findItem(R.id.menu_main_stop_listening).isVisible = state == State.Waiting
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected")
        return when (item.itemId) {
            R.id.menu_main_connect -> {
                agent = ClientAgent(this, CommHandler(this))
                (agent as ClientAgent).connect()
                true
            }
            R.id.menu_main_disconnect -> {
                disconnect()
                true
            }
            R.id.menu_main_accept_connection -> {
                agent = ServerAgent(this, CommHandler(this))
                (agent as ServerAgent).start(adapter)
                true
            }
            R.id.menu_main_stop_listening -> {
                (agent as ServerAgent?)?.stop()
                true
            }
            R.id.menu_main_clear_connection -> {
                chatLogAdapter.clear()
                true
            }
            R.id.menu_main_about -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.about_dialog_title)
                        .setMessage(R.string.about_dialog_content)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(reqCode: Int, resCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resCode, data)
        Log.d(TAG, "onActivityResult: reqCode=$reqCode resCode=$resCode")
        initializer.onActivityResult(reqCode, resCode, data) // delegate
        agent?.onActivityResult(reqCode, resCode, data) // delegate
    }

    fun onClickSendButton(v: View?) {
        Log.d(TAG, "onClickSendButton")
        val content = main_input.text.toString().trim { it <= ' ' }
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_message, Toast.LENGTH_SHORT).show()
            return
        }
        messageSeq++
        val time = System.currentTimeMillis()
        val message = ChatMessage(messageSeq, time, content, adapter.name)
        agent!!.send(message)
        chatLogAdapter.add(message)
        chatLogAdapter.notifyDataSetChanged()
        main_logview.smoothScrollToPosition(chatLog.size)
        main_input.editableText.clear()
    }

    fun onClickSoundButton(v: View) {
        Log.d(TAG, "onClickSoundButton")
        val time = System.currentTimeMillis()
        val message = ChatMessage(messageSeq, time, null, adapter.name, true)
        agent!!.send(message)
    }

    fun setState(state: State) {
        setState(state, null)
    }

    fun setState(state: State, arg: String?) {
        this.state = state
        main_input.isEnabled = state == State.Connected
        main_button.isEnabled = state == State.Connected
        when (state) {
            State.Initializing, State.Disconnected -> main_status.setText(R.string.main_status_disconnected)
            State.Connecting -> main_status.text = getString(R.string.main_status_connecting_to, arg)
            State.Connected -> {
                main_status.text = getString(R.string.main_status_connected_to, arg)
                soundPlayer.playConnected()
            }
            State.Waiting -> main_status.setText(R.string.main_status_listening_for_incoming_connection)
        }
        invalidateOptionsMenu()
    }

    fun setProgress(isConnecting: Boolean) {
        main_progress.isIndeterminate = isConnecting
    }

    fun showMessage(message: ChatMessage) {
        if (message.sound) {
            soundPlayer.playRing()
        } else {
            chatLogAdapter.add(message)
            chatLogAdapter.notifyDataSetChanged()
            main_logview.smoothScrollToPosition(chatLogAdapter.count)
        }
    }

    private fun disconnect() {
        Log.d(TAG, "disconnect")
        agent?.close()
        agent = null
        setState(State.Disconnected)
        soundPlayer.playDisconnected()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        @JvmField
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}