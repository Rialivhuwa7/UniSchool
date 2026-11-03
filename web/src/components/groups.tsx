import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ok, type Schemas } from "@unischool/api";
import { api, errorMessage } from "../lib/api";
import { useToast } from "./toast";
import {
  Badge,
  Button,
  Card,
  Checkbox,
  Empty,
  Page,
  QueryView,
  Textarea,
  cx,
  formatDateTime,
} from "./ui";

/*
 * Class and team chat groups, shared by parents and teachers. Both roles use the same
 * /api/groups endpoints; the backend decides who may post (moderators only in ANNOUNCE groups).
 */

type Group = Schemas["GroupView"];
const LIST_KEY = ["groups", "list"];

export function GroupsPage({
  intro = "Chat with other parents in your children’s classes and teams.",
}: {
  intro?: string;
}) {
  const groups = useQuery({
    queryKey: LIST_KEY,
    queryFn: () => ok(api.GET("/api/groups")),
  });
  const [openId, setOpenId] = useState<number | null>(null);
  const open = groups.data?.find((g) => g.id === openId) ?? groups.data?.[0];

  return (
    <Page title="Groups" intro={intro}>
      <QueryView query={groups}>
        {(list) =>
          list.length === 0 || !open ? (
            <Empty>You're not in any groups yet.</Empty>
          ) : (
            <div className="grid gap-6 md:grid-cols-[280px_1fr]">
              <nav aria-label="Your groups" className="space-y-2">
                {list.map((g) => (
                  <button
                    key={g.id}
                    type="button"
                    onClick={() => setOpenId(g.id)}
                    className={cx(
                      "w-full rounded-xl border p-3 text-left",
                      g.id === open.id
                        ? "border-blue bg-blue-soft"
                        : "border-line bg-surface hover:bg-surface-2",
                    )}
                  >
                    <span className="flex items-center justify-between gap-2">
                      <span className="font-semibold">{g.name}</span>
                      {g.unread > 0 && g.id !== open.id && (
                        <Badge tone="blue">{g.unread}</Badge>
                      )}
                    </span>
                    <span className="block text-sm text-ink-3">
                      {g.school}
                      {g.forChildren?.length
                        ? ` · ${g.forChildren.join(", ")}`
                        : ""}
                      {g.muted && " · muted"}
                    </span>
                  </button>
                ))}
              </nav>
              <Conversation key={open.id} group={open} />
            </div>
          )
        }
      </QueryView>
    </Page>
  );
}

function Conversation({ group }: { group: Group }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [body, setBody] = useState("");
  const [muted, setMuted] = useState(group.muted);
  const messages = useQuery({
    queryKey: ["groups", group.id, "messages"],
    queryFn: () =>
      ok(
        api.GET("/api/groups/{id}/messages", {
          params: { path: { id: group.id } },
        }),
      ),
  });

  // Opening a group marks it read on the server, so refresh the unread counts.
  const loadedAt = messages.dataUpdatedAt;
  useEffect(() => {
    if (loadedAt)
      queryClient.invalidateQueries({ queryKey: LIST_KEY, exact: true });
  }, [loadedAt, queryClient]);

  const onError = (e: unknown) => toast(errorMessage(e), "bad");
  const post = useMutation({
    mutationFn: () =>
      ok(
        api.POST("/api/groups/{id}/messages", {
          params: { path: { id: group.id } },
          body: { body: body.trim() },
        }),
      ),
    onSuccess: (res) => {
      setBody("");
      toast(res.notice ?? "Message sent.");
      queryClient.invalidateQueries({
        queryKey: ["groups", group.id, "messages"],
      });
    },
    onError,
  });
  const mute = useMutation({
    mutationFn: (muted: boolean) =>
      ok(
        api.PUT("/api/groups/{id}/mute", {
          params: { path: { id: group.id } },
          body: { muted },
        }),
      ),
    onSuccess: (g) => {
      toast(
        g.muted
          ? `${g.name} is muted. You won't get notifications.`
          : `You'll get notifications from ${g.name} again.`,
      );
      queryClient.setQueryData<Group[]>(LIST_KEY, (all) =>
        all?.map((x) => (x.id === g.id ? g : x)),
      );
    },
    onError: (e) => {
      setMuted(group.muted);
      onError(e);
    },
  });
  const report = useMutation({
    mutationFn: (messageId: number) =>
      ok(
        api.POST("/api/groups/messages/{messageId}/report", {
          params: { path: { messageId } },
        }),
      ),
    onSuccess: () => toast("Thanks. The moderator will look at it."),
    onError,
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    if (body.trim()) post.mutate();
  }

  return (
    <Card
      title={group.name}
      actions={
        <Checkbox
          label="Mute"
          checked={muted}
          disabled={mute.isPending}
          onChange={(e) => {
            setMuted(e.target.checked);
            mute.mutate(e.target.checked);
          }}
        />
      }
    >
      <p className="-mt-3 mb-4 text-sm text-ink-3">
        {group.members} member{group.members === 1 ? "" : "s"} · Moderated by{" "}
        {group.moderator}
        {group.mode === "ANNOUNCE" && " · Announcements only"}
      </p>
      <QueryView query={messages}>
        {(list) =>
          list.length === 0 ? (
            <Empty>No messages yet.</Empty>
          ) : (
            <ul className="space-y-3">
              {list.map((m) => (
                <li
                  key={m.id}
                  className={cx(
                    "max-w-[85%] rounded-2xl px-4 py-2.5",
                    m.mine ? "ml-auto bg-blue-soft" : "bg-surface-2",
                  )}
                >
                  <div className="flex flex-wrap items-center gap-2 text-sm">
                    <span className="font-semibold">
                      {m.mine ? "You" : m.author}
                    </span>
                    {m.role && <Badge>{m.role}</Badge>}
                    <span className="text-ink-3">
                      {formatDateTime(m.sentAt)}
                    </span>
                    {m.scheduled && (
                      <Badge tone="warn">
                        Arrives {formatDateTime(m.deliverAt)}
                      </Badge>
                    )}
                  </div>
                  <p className="mt-1 whitespace-pre-line">{m.body}</p>
                  {!m.mine && (
                    <button
                      type="button"
                      className="mt-1 text-xs font-semibold text-ink-3 hover:text-bad"
                      disabled={report.isPending}
                      onClick={() =>
                        window.confirm(
                          "Report this message to the moderator?",
                        ) && report.mutate(m.id)
                      }
                    >
                      Report
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )
        }
      </QueryView>
      {group.canPost ? (
        <form
          onSubmit={submit}
          className="mt-4 space-y-2 border-t border-line pt-4"
        >
          <Textarea
            rows={2}
            maxLength={2000}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Write a message"
            aria-label="Message"
          />
          <Button type="submit" busy={post.isPending} disabled={!body.trim()}>
            Send
          </Button>
        </form>
      ) : (
        <p className="mt-4 border-t border-line pt-4 text-sm text-ink-3">
          Only the moderator posts in this group.
        </p>
      )}
    </Card>
  );
}
