"""Windows-first command-line interface. Usage: coddy --help."""

from __future__ import annotations

import argparse
import os
import sys

from coddy.config import Settings
from coddy.curator import propose_from_message
from coddy.memory import MemoryStore
from coddy.pending import PendingQueue


def make_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog='coddy', description='Personal assistant with Obsidian memory')
    commands = parser.add_subparsers(dest='command', required=True)
    commands.add_parser('init', help='Initialize the vault configured by CODDY_VAULT_PATH')
    chat = commands.add_parser('chat', help='Chat with Coddy using the OpenAI Agents SDK')
    chat.add_argument('--speak', action='store_true', help='Read replies aloud with a Windows installed voice')
    remember = commands.add_parser('remember', help='Save an explicitly requested memory')
    remember.add_argument('category', choices=['fact', 'preference', 'decision', 'experience', 'project', 'lesson'])
    remember.add_argument('title')
    remember.add_argument('content')
    search = commands.add_parser('search', help='Search approved vault memories')
    search.add_argument('query')
    commands.add_parser('recent', help='Show recent approved memories')
    commands.add_parser('review', help='View proposed memories awaiting approval')
    approve = commands.add_parser('approve', help='Approve a specific proposed memory')
    approve.add_argument('id')
    reject = commands.add_parser('reject', help='Reject a proposed memory')
    reject.add_argument('id')
    archive = commands.add_parser('archive', help='Remove an approved memory from active search, keeping its history')
    archive.add_argument('id')
    commands.add_parser('voices', help='List voices installed in Windows')
    return parser


def main(argv: list[str] | None = None) -> int:
    args = make_parser().parse_args(argv)
    try:
        if args.command == 'voices':
            from coddy.voice import list_windows_voices
            for voice in list_windows_voices():
                print(voice)
            return 0
        settings = Settings.from_environment()
        vault = MemoryStore(settings.vault_path)
        queue = PendingQueue(settings.data_path / 'pending.json')
        if args.command == 'init':
            vault.initialize()
            print(f'Obsidian memory vault ready: {vault.root}')
            return 0
        if not vault.is_initialized():
            raise ValueError('No Coddy vault found at configured path. Run `coddy init` first.')
        if args.command == 'remember':
            m = vault.remember(args.category, args.title, args.content)
            print(f'Memory saved: {m.title} [{m.id}]')
        elif args.command == 'search':
            for memory in vault.search(args.query):
                print(f'[{memory.id}] ({memory.category}) {memory.title}: {memory.content}')
        elif args.command == 'recent':
            for memory in vault.list_recent():
                print(f'[{memory.id}] ({memory.category}) {memory.title}')
        elif args.command == 'review':
            for proposal in queue.list_pending():
                print(f"[{proposal['id']}] ({proposal['category']}) {proposal['title']}: {proposal['content']}")
        elif args.command == 'approve':
            m = queue.approve(args.id, vault)
            print(f'Approved: {m.title} [{m.id}]')
        elif args.command == 'reject':
            print('Rejected.' if queue.reject(args.id) else 'No matching suggestion.')
        elif args.command == 'archive':
            print('Archived.' if vault.archive(args.id) else 'No matching approved memory.')
        elif args.command == 'chat':
            if not os.environ.get('OPENAI_API_KEY'):
                raise ValueError('OPENAI_API_KEY is not set. Create an API key in the OpenAI Platform.')
            from coddy.agent import CoddyChat
            from coddy.voice import speak
            chat = CoddyChat(settings, vault)
            print('Coddy is ready. Type /exit to leave. Use `coddy review` to inspect suggested memories.')
            while True:
                try:
                    message = input('You: ').strip()
                except (EOFError, KeyboardInterrupt):
                    print('\nGoodbye.')
                    break
                if message.casefold() in {'/exit', '/quit'}:
                    break
                if not message:
                    continue
                try:
                    answer = chat.reply(message)
                    print(f'Coddy: {answer}')
                    if args.speak:
                        try:
                            speak(answer, settings.voice_name)
                        except Exception as exc:
                            print(f'[Voice unavailable: {exc}]', file=sys.stderr)
                    if settings.auto_suggest:
                        count = propose_from_message(message, queue, settings.model)
                        if count:
                            print(f'[Memory suggestions awaiting approval: {count}]')
                except Exception as exc:
                    print(f'[Chat error: {exc}]', file=sys.stderr)
        return 0
    except (ValueError, RuntimeError, OSError) as exc:
        print(f'Coddy: {exc}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
