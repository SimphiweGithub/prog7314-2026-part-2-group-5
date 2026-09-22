/*
 * Code Attribution No 41
 * This method was taken from "Unit Testing in ASP.NET Core Web API Using xUnit and Moq"
 * https://www.c-sharpcorner.com/article/unit-testing-in-asp-net-core-web-api-using-xunit-and-moq/
 * C# Corner
 */

using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Controllers;
using PulseSync.Api.Models;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

public class NotesControllerTests
{
    private readonly IDataStore _dataStore;
    private readonly NotesController _controller;

    public NotesControllerTests()
    {
        _dataStore = new InMemoryDataStore();
        _controller = new NotesController(_dataStore).SignedInAs("demo-user");
    }

    [Fact]
    public void CreateNote_WithValidContent_Returns201CreatedAndPersists()
    {
        // Arrange
        var request = new CreateNoteRequestDto(
            LocalId: "local-note-101",
            ArticleId: "nsfas-2027",
            Title: "NSFAS Required Documents",
            BodyHtml: "<p>Remember to certify ID copy before Friday.</p>",
            PlainText: "Remember to certify ID copy before Friday.",
            Tags: new List<string> { "urgent", "nsfas" },
            IsVaultNote: false,
            UpdatedAt: DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        );

        // Act
        var result = _controller.CreateNote(request);

        // Assert
        result.Result.Should().BeOfType<CreatedAtActionResult>();
        var created = (result.Result as CreatedAtActionResult)!.Value as NoteDto;

        created.Should().NotBeNull();
        created!.NoteId.Should().NotBeNullOrWhiteSpace();
        created.Title.Should().Be("NSFAS Required Documents");
        created.Tags.Should().Contain("urgent");

        // Verify retrieval
        var notes = _dataStore.GetNotesForArticle("demo-user", "nsfas-2027");
        notes.Should().ContainSingle(n => n.NoteId == created.NoteId);
    }

    [Fact]
    public void CreateNote_WithEmptyTitleAndBody_ReturnsBadRequest()
    {
        // Arrange
        var request = new CreateNoteRequestDto(
            LocalId: "local-empty-1",
            Title: "",
            BodyHtml: ""
        );

        // Act
        var result = _controller.CreateNote(request);

        // Assert
        result.Result.Should().BeOfType<BadRequestObjectResult>();
    }

    [Fact]
    public void UpdateNote_WithValidId_UpdatesAndReturnsNote()
    {
        // Arrange
        var createRequest = new CreateNoteRequestDto(
            LocalId: "local-note-update",
            ArticleId: "eskom-stage-2",
            Title: "Original Title",
            BodyHtml: "<p>Original Body</p>",
            Tags: new List<string> { "eskom" }
        );
        var createdNote = _dataStore.CreateNote("demo-user", createRequest);

        var updateRequest = new UpdateNoteRequestDto(
            Title: "Updated Title After Outage",
            BodyHtml: "<p>Updated Body text.</p>",
            PlainText: "Updated Body text.",
            Tags: new List<string> { "eskom", "resolved" },
            IsPinned: true,
            UpdatedAt: DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        );

        // Act
        var result = _controller.UpdateNote(createdNote.NoteId, updateRequest);

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var updated = (result.Result as OkObjectResult)!.Value as NoteDto;

        updated.Should().NotBeNull();
        updated!.Title.Should().Be("Updated Title After Outage");
        updated.Tags.Should().Contain("resolved");
        updated.IsPinned.Should().BeTrue();
    }

    [Fact]
    public void DeleteNote_WithValidId_ReturnsNoContent()
    {
        // Arrange
        var createRequest = new CreateNoteRequestDto(
            LocalId: "local-note-delete",
            Title: "Note to Delete",
            BodyHtml: "<p>Delete me</p>"
        );
        var created = _dataStore.CreateNote("demo-user", createRequest);

        // Act
        var result = _controller.DeleteNote(created.NoteId);

        // Assert
        result.Should().BeOfType<NoContentResult>();

        // Verify note is gone
        var notes = _dataStore.GetNotes("demo-user", null, null);
        notes.Should().NotContain(n => n.NoteId == created.NoteId);
    }
}
