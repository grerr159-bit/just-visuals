#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для исправления кодировки в Java файлах
Автоматически определяет и исправляет проблемы с кодировкой
"""

import os
import sys
from pathlib import Path

def fix_file_encoding(file_path):
    """Исправляет кодировку файла"""
    try:
        # Пробуем прочитать как UTF-8
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Проверяем наличие кракозябр
        if '�' in content or '???' in content:
            print(f"  Обнаружены проблемы в: {file_path}")
            
            # Пробуем разные кодировки
            encodings = ['windows-1251', 'cp866', 'iso-8859-5', 'koi8-r']
            
            for encoding in encodings:
                try:
                    with open(file_path, 'r', encoding=encoding) as f:
                        decoded_content = f.read()
                    
                    # Проверяем, исправилось ли
                    if '�' not in decoded_content and '???' not in decoded_content:
                        # Сохраняем в UTF-8
                        with open(file_path, 'w', encoding='utf-8') as f:
                            f.write(decoded_content)
                        print(f"  ✓ Исправлено с помощью {encoding}")
                        return True
                except:
                    continue
            
            print(f"  ✗ Не удалось автоматически исправить")
            return False
        
        return False
    except Exception as e:
        print(f"  ✗ Ошибка: {e}")
        return False

def main():
    print("╔════════════════════════════════════════════════════════╗")
    print("║   Скрипт исправления кодировки Java файлов            ║")
    print("╚════════════════════════════════════════════════════════╝")
    print()
    
    total_files = 0
    fixed_files = 0
    
    # Ищем все Java файлы
    java_files = list(Path('.').rglob('*.java'))
    
    # Исключаем build и .gradle
    java_files = [f for f in java_files if 'build' not in str(f) and '.gradle' not in str(f)]
    
    print(f"Найдено файлов: {len(java_files)}")
    print()
    
    for file_path in java_files:
        total_files += 1
        print(f"[{total_files}/{len(java_files)}] {file_path}")
        
        if fix_file_encoding(file_path):
            fixed_files += 1
    
    print()
    print("╔════════════════════════════════════════════════════════╗")
    print("║                    РЕЗУЛЬТАТЫ                          ║")
    print("╠════════════════════════════════════════════════════════╣")
    print(f"║  Всего проверено:  {total_files:3d} файлов                        ║")
    print(f"║  Исправлено:       {fixed_files:3d} файлов                        ║")
    print("╚════════════════════════════════════════════════════════╝")
    print()
    
    if fixed_files > 0:
        print("✓ Исправление завершено успешно!")
        print("  Рекомендуется пересобрать проект")
    else:
        print("✓ Все файлы уже в правильной кодировке!")

if __name__ == '__main__':
    main()
    input("\nНажмите Enter для выхода...")
