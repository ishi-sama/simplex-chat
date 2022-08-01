package chat.simplex.app.views.chat.group

import InfoRow
import SectionDivider
import SectionItemView
import SectionSpacer
import SectionView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.app.R
import chat.simplex.app.model.*
import chat.simplex.app.ui.theme.*
import chat.simplex.app.views.chat.*
import chat.simplex.app.views.chatlist.populateGroupMembers
import chat.simplex.app.views.helpers.*

@Composable
fun GroupChatInfoView(groupInfo: GroupInfo, chatModel: ChatModel, close: () -> Unit) {
  BackHandler(onBack = close)
  val chat = chatModel.chats.firstOrNull { it.id == chatModel.chatId.value }
  if (chat != null) {
    GroupChatInfoLayout(
      chat,
      groupInfo,
      members = chatModel.groupMembers.sortedBy { it.displayName.lowercase() },
      addMembers = {
        withApi {
          populateGroupMembers(groupInfo, chatModel)
          ModalManager.shared.showCustomModal { close ->
            ModalView(
              close = close, modifier = Modifier,
              background = if (isSystemInDarkTheme()) MaterialTheme.colors.background else SettingsBackgroundLight
            ) {
              AddGroupMembersView(groupInfo, chatModel, close)
            }
          }
        }
      },
      showMemberInfo = { member ->
        withApi {
          val connStats = chatModel.controller.apiGroupMemberInfo(groupInfo.groupId, member.groupMemberId)
          ModalManager.shared.showCustomModal { close ->
            ModalView(
              close = close, modifier = Modifier,
              background = if (isSystemInDarkTheme()) MaterialTheme.colors.background else SettingsBackgroundLight
            ) {
              GroupMemberInfoView(groupInfo, member, connStats, chatModel, close)
            }
          }
        }
      },
      deleteGroup = { deleteGroupDialog(chat.chatInfo, chatModel, close) },
      clearChat = { clearChatDialog(chat.chatInfo, chatModel, close) },
      leaveGroup = { leaveGroupDialog(groupInfo, chatModel, close) }
    )
  }
}

fun deleteGroupDialog(chatInfo: ChatInfo, chatModel: ChatModel, close: (() -> Unit)? = null) {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.delete_group_question),
    text = generalGetString(R.string.delete_group_for_all_members_cannot_undo_warning),
    confirmText = generalGetString(R.string.delete_verb),
    onConfirm = {
      withApi {
        val r = chatModel.controller.apiDeleteChat(chatInfo.chatType, chatInfo.apiId)
        if (r) {
          chatModel.removeChat(chatInfo.id)
          chatModel.chatId.value = null
          chatModel.controller.ntfManager.cancelNotificationsForChat(chatInfo.id)
          close?.invoke()
        }
      }
    }
  )
}

fun leaveGroupDialog(groupInfo: GroupInfo, chatModel: ChatModel, close: (() -> Unit)? = null) {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.leave_group_question),
    text = generalGetString(R.string.you_will_stop_receiving_messages_from_this_group_chat_history_will_be_preserved),
    confirmText = generalGetString(R.string.leave_group_button),
    onConfirm = {
      withApi {
        chatModel.controller.leaveGroup(groupInfo.groupId)
        close?.invoke()
      }
    }
  )
}

@Composable
fun GroupChatInfoLayout(
  chat: Chat,
  groupInfo: GroupInfo,
  members: List<GroupMember>,
  addMembers: () -> Unit,
  showMemberInfo: (GroupMember) -> Unit,
  deleteGroup: () -> Unit,
  clearChat: () -> Unit,
  leaveGroup: () -> Unit,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center
    ) {
      ChatInfoHeader(chat.chatInfo)
    }
    SectionSpacer()

    SectionView(title = String.format(generalGetString(R.string.group_info_section_title_num_members), members.count() + 1)) {
      if (groupInfo.canAddMembers) {
        SectionItemView {
          AddMembersButton(addMembers)
        }
        SectionDivider()
      }
      SectionItemView(height = 50.dp) {
        MemberRow(groupInfo.membership, user = true)
      }
      SectionDivider()
      MembersList(members, showMemberInfo)
    }
    SectionSpacer()

    SectionView {
      SectionItemView {
        ClearChatButton(clearChat)
      }
      if (groupInfo.canDelete) {
        SectionDivider()
        SectionItemView {
          DeleteGroupButton(deleteGroup)
        }
      }
      if (groupInfo.membership.memberCurrent) {
        SectionDivider()
        SectionItemView {
          LeaveGroupButton(leaveGroup)
        }
      }
    }
    SectionSpacer()

    SectionView(title = stringResource(R.string.section_title_for_console)) {
      InfoRow(stringResource(R.string.info_row_local_name), groupInfo.localDisplayName)
      SectionDivider()
      InfoRow(stringResource(R.string.info_row_database_id), groupInfo.apiId.toString())
    }
    SectionSpacer()
  }
}

@Composable
fun AddMembersButton(addMembers: () -> Unit) {
  Row(
    Modifier
      .fillMaxSize()
      .clickable { addMembers() },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Add,
      stringResource(R.string.button_add_members),
      tint = MaterialTheme.colors.primary
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_add_members), color = MaterialTheme.colors.primary)
  }
}

@Composable
fun MembersList(members: List<GroupMember>, showMemberInfo: (GroupMember) -> Unit) {
  Column {
    members.forEachIndexed { index, member ->
      SectionItemView(height = 50.dp) {
        MemberRow(member, showMemberInfo)
      }
      if (index < members.lastIndex) {
        SectionDivider()
      }
    }
  }
}

@Composable
fun MemberRow(member: GroupMember, showMemberInfo: ((GroupMember) -> Unit)? = null, user: Boolean = false) {
  val modifier = if (showMemberInfo != null) Modifier.clickable { showMemberInfo(member) } else Modifier
  Row(
    modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      ProfileImage(size = 46.dp, member.image)
      Column {
        Text(member.chatViewName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val s = member.memberStatus.shortText
        val statusDescr = if (user) String.format(generalGetString(R.string.group_info_member_you), s) else s
        Text(
          statusDescr,
          color = HighOrLowlight,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    val role = member.memberRole
    if (role == GroupMemberRole.Owner || role == GroupMemberRole.Admin) {
      Text(role.text, color = HighOrLowlight)
    }
  }
}

@Composable
fun LeaveGroupButton(leaveGroup: () -> Unit) {
  Row(
    Modifier
      .fillMaxSize()
      .clickable { leaveGroup() },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Logout,
      stringResource(R.string.button_leave_group),
      tint = Color.Red
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_leave_group), color = Color.Red)
  }
}

@Composable
fun DeleteGroupButton(deleteGroup: () -> Unit) {
  Row(
    Modifier
      .fillMaxSize()
      .clickable { deleteGroup() },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Delete,
      stringResource(R.string.button_delete_group),
      tint = Color.Red
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_delete_group), color = Color.Red)
  }
}

@Preview
@Composable
fun PreviewGroupChatInfoLayout() {
  SimpleXTheme {
    GroupChatInfoLayout(
      chat = Chat(
        chatInfo = ChatInfo.Direct.sampleData,
        chatItems = arrayListOf(),
        serverInfo = Chat.ServerInfo(Chat.NetworkStatus.Error("agent BROKER TIMEOUT"))
      ),
      groupInfo = GroupInfo.sampleData,
      members = listOf(GroupMember.sampleData, GroupMember.sampleData, GroupMember.sampleData),
      addMembers = {}, showMemberInfo = {}, deleteGroup = {}, clearChat = {}, leaveGroup = {}
    )
  }
}