/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
 * Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
 *
 * The CRUD actions, route parameter binding, and HTTP response mapping were adapted from:
 *
 * C# Corner (2024) CRUD Operations in ASP.NET Core Web API. [online]
 * Available at: https://www.c-sharpcorner.com/article/crud-operations-in-asp-net-core-web-api/
 * [Accessed 21 September 2026].
 *
 * Google DeepMind Antigravity (2026) Contextual Note CRUD & Tag Aggregation Logic.
 * ---------------------------------------------------------------------
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Models;
using PulseSync.Api.Services;

namespace PulseSync.Api.Controllers;

[ApiController]
[Route("api/v1/notes")]
public class NotesController : ControllerBase
{
    private readonly IDataStore _dataStore;

    public NotesController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    private string CurrentUserId =>
        User?.FindFirstValue(ClaimTypes.NameIdentifier) ??
        User?.FindFirstValue("userId") ??
        "demo-user";

    [HttpGet]
    public ActionResult<List<NoteDto>> GetNotes([FromQuery] string? tag = null, [FromQuery] string? q = null)
    {
        var notes = _dataStore.GetNotes(CurrentUserId, tag, q);
        return Ok(notes.ToList());
    }

    [HttpGet("article/{articleId}")]
    public ActionResult<List<NoteDto>> GetNotesForArticle(string articleId)
    {
        var notes = _dataStore.GetNotesForArticle(CurrentUserId, articleId);
        return Ok(notes.ToList());
    }

    [HttpPost]
    public ActionResult<NoteDto> CreateNote([FromBody] CreateNoteRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.Title) && string.IsNullOrWhiteSpace(request.BodyHtml))
        {
            return BadRequest(new { message = "Note title or body cannot be empty." });
        }

        var note = _dataStore.CreateNote(CurrentUserId, request);
        return CreatedAtAction(nameof(GetNotes), new { id = note.NoteId }, note);
    }

    [HttpPut("{id}")]
    public ActionResult<NoteDto> UpdateNote(string id, [FromBody] UpdateNoteRequestDto request)
    {
        var updated = _dataStore.UpdateNote(CurrentUserId, id, request);
        if (updated == null)
        {
            return NotFound(new { message = $"Note with ID '{id}' was not found." });
        }

        return Ok(updated);
    }

    [HttpDelete("{id}")]
    public IActionResult DeleteNote(string id)
    {
        var deleted = _dataStore.DeleteNote(CurrentUserId, id);
        if (!deleted)
        {
            return NotFound(new { message = $"Note with ID '{id}' was not found." });
        }

        return NoContent();
    }

    [HttpGet("tags")]
    public ActionResult<List<TagDto>> GetTags()
    {
        var tags = _dataStore.GetNoteTags(CurrentUserId);
        return Ok(tags.ToList());
    }
}
