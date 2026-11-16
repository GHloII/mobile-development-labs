<?php
declare(strict_types=1);
// Controller: reject malformed requests before invoking the calculation model.
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

function reply(int $status, array $body): never {
    http_response_code($status);
    echo json_encode($body, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Allow: POST');
    reply(405, ['ok' => false, 'error' => 'method_not_allowed']);
}
if (!str_starts_with(strtolower($_SERVER['CONTENT_TYPE'] ?? ''), 'application/x-www-form-urlencoded')) {
    reply(415, ['ok' => false, 'error' => 'unsupported_media_type']);
}
if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0) > 1024) {
    reply(413, ['ok' => false, 'error' => 'body_too_large']);
}

$values = [];
foreach (['a', 'b', 'h'] as $key) {
    $raw = $_POST[$key] ?? null;
    if (!is_string($raw) || !is_numeric($raw)) {
        reply(422, ['ok' => false, 'error' => 'invalid_dimensions']);
    }
    $number = (float)$raw;
    if (!is_finite($number) || $number <= 0 || $number > 1000000) {
        reply(422, ['ok' => false, 'error' => 'invalid_dimensions']);
    }
    $values[] = $number;
}

// Model: apex over the centre of a rectangular base, two pairs of slant heights.
[$a, $b, $h] = $values;
$surface = $a * $b + $a * hypot($h, $b / 2) + $b * hypot($h, $a / 2);
$volume = $a * $b * $h / 3;
// Underflow is also rejected: an unrepresentable positive result must not become zero.
if (!is_finite($surface) || !is_finite($volume) || $surface <= 0 || $volume <= 0) {
    reply(422, ['ok' => false, 'error' => 'unrepresentable_result']);
}
reply(200, ['ok' => true, 'res' => ['surface' => $surface, 'volume' => $volume]]);
