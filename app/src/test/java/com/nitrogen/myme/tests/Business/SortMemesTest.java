package com.nitrogen.myme.tests.Business;

import com.nitrogen.myme.business.SortMemes;
import com.nitrogen.myme.objects.Meme;
import com.nitrogen.myme.objects.Tag;
import com.nitrogen.myme.persistence.MemesPersistence;
import com.nitrogen.myme.persistence.TagsPersistence;
import com.nitrogen.myme.persistence.stubs.MemesPersistenceStub;
import com.nitrogen.myme.persistence.stubs.TagsPersistenceStub;

import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SortMemesTest {
    private List<Meme> memes;
    private MemesPersistence memesPersistenceStub;
    private Tag tag1 = new Tag("tag1");
    private Tag tag2 = new Tag("tag2");

    @Before
    public void setUp() {
        System.out.println("Starting tests for SearchTags.\n");
        // stub database
        TagsPersistence tagsPersistenceStub = mock(TagsPersistenceStub.class);
        List<Tag> tags = new LinkedList<>();
        tags.add(tag1);
        tags.add(tag2);
        when(tagsPersistenceStub.getTags()).thenReturn(tags);

        List<Meme> memes = new ArrayList<>(); // implementation specific
        Meme fav1 = new Meme("fav1");
        fav1.setFavourite(true);
        memes.add(fav1);
        Meme fav2 = new Meme("fav2");
        fav2.addTag(tag1);
        fav2.setFavourite(true);
        memes.add(fav2);
        Meme fav3 = new Meme("fav3");
        fav3.addTag(tag1);
        fav3.addTag(tag2);
        fav3.setFavourite(true);
        memes.add(fav3);

        memesPersistenceStub = mock(MemesPersistenceStub.class);
        when(memesPersistenceStub.getMemes()).thenReturn(memes);

    }

    @Test
    public void testSortMemesByRelevance_emptyMemeList() {
        // sorting by relevance on empty meme list
        System.out.println("Testing getTagsFromMemes() with an empty meme list");
        SortMemes sm = new SortMemes(new LinkedList<Meme>(), memesPersistenceStub);
        assertEquals(0, sm.sortByRelevance().size());
    }

    @Test
    public void testSortMemesByRelevance_multipleMemes() {
        System.out.println("Testing sortByRelevance() on multiple memes.");

        List<Meme> memes = new ArrayList<>(); // implementation specific
        Meme meme1 = new Meme("meme1");
        memes.add(meme1);
        Meme meme2 = new Meme("meme2");
        meme2.addTag(tag1);
        memes.add(meme2);
        Meme meme3 = new Meme("meme3");
        meme3.addTag(tag1);
        meme3.addTag(tag2);
        memes.add(meme3);

        //check memes are in order inserted
        assertTrue(memes.get(0).equals(meme1));
        assertTrue(memes.get(1).equals(meme2));
        assertTrue(memes.get(2).equals(meme3));

        SortMemes sm = new SortMemes(memes, memesPersistenceStub);
        sm.sortByRelevance();

        //check memes are in relevance order based on favorites
        assertTrue(memes.get(0).equals(meme3));
        assertTrue(memes.get(1).equals(meme2));
        assertTrue(memes.get(2).equals(meme1));
    }

    @Test
    public void testSortMemesByRelevance_sortedMemesAtBottom() {
        System.out.println("Testing sortByRelevance() sorts favorite memes to the bottom.");

        List<Meme> memes = new ArrayList<>(); // implementation specific
        Meme meme1 = new Meme("meme1");
        memes.add(meme1);
        Meme meme2 = new Meme("meme2");
        meme2.addTag(tag1);
        memes.add(meme2);
        Meme meme3 = new Meme("meme3");
        meme3.addTag(tag1);
        meme3.addTag(tag2);
        memes.add(meme3);

        //check memes are in order inserted
        assertTrue(memes.get(0).equals(meme1));
        assertTrue(memes.get(1).equals(meme2));
        assertTrue(memes.get(2).equals(meme3));

        meme1.setFavourite(true);

        SortMemes sm = new SortMemes(memes, memesPersistenceStub);
        sm.sortByRelevance();

        //check memes are in relevance order based on favorites
        assertTrue(memes.get(2).equals(meme1));
    }


    @After
    public void tearDown() {
        System.out.println("\nFinished tests.\n");
    }
}

